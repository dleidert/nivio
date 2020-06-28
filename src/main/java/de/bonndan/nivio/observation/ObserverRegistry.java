package de.bonndan.nivio.observation;

import de.bonndan.nivio.IndexEvent;
import de.bonndan.nivio.ProcessingFinishedEvent;
import de.bonndan.nivio.input.LandscapeDescriptionFactory;
import de.bonndan.nivio.input.LandscapeUrlsFactory;
import de.bonndan.nivio.input.dto.LandscapeDescription;
import de.bonndan.nivio.model.Landscape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service to register landscapes to observe description source changes.
 */
@Service
public class ObserverRegistry implements ApplicationListener<ProcessingFinishedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObserverRegistry.class);

    private final Map<String, LandscapeObserverPool> observerMap = new ConcurrentHashMap<>();
    private final URLObserverFactory urlObserverFactory;
    private final LandscapeDescriptionFactory landscapeDescriptionFactory;
    private final LandscapeUrlsFactory landscapeUrlsFactory;
    private final ApplicationEventPublisher publisher;

    public ObserverRegistry(URLObserverFactory urlObserverFactory,
                            LandscapeDescriptionFactory landscapeDescriptionFactory,
                            LandscapeUrlsFactory landscapeUrlsFactory,
                            ApplicationEventPublisher publisher
    ) {
        this.urlObserverFactory = urlObserverFactory;
        this.landscapeDescriptionFactory = landscapeDescriptionFactory;
        this.landscapeUrlsFactory = landscapeUrlsFactory;
        this.publisher = publisher;
    }

    /**
     * Landscape are registered for observation here.
     * <p>
     * On processing success, {@link ProcessingFinishedEvent} is fired and read here to register the landscape.
     */
    @Override
    public void onApplicationEvent(ProcessingFinishedEvent event) {
        LandscapeDescription from = (LandscapeDescription) event.getSource();
        if (from == null) {
            LOGGER.warn("Landscape {} could not be registered for observation", event.getLandscape().getIdentifier());
            return;
        }

        URL sourceUrl = null;
        try {
            sourceUrl = new URL(from.getSource());
        } catch (MalformedURLException e) {
            LOGGER.info("Landscape {} does not seem to have a source", from.getIdentifier());
        }

        List<URL> landscapeSourceLocations = landscapeUrlsFactory.getLandscapeSourceLocations(from, sourceUrl);
        setLandscapeUrls(from, landscapeSourceLocations);
        LOGGER.info("Registered landscape {} for observation with {} urls.", from, landscapeSourceLocations.size());
    }

    /**
     * Polls for changes in landscapes.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void poll() {
        LOGGER.info("Polling {} landscapes for changes.", observerMap.size());
        observerMap.entrySet().parallelStream().forEach(e -> check(e.getValue()));
    }

    /**
     * @return the currently observed landscapes.
     */
    public Set<String> getObservedLandscapes() {
        return observerMap.keySet();
    }

    private void setLandscapeUrls(Landscape landscape, List<URL> urls) {
        observerMap.put(
                landscape.getIdentifier(),
                new LandscapeObserverPool(
                        landscape,
                        urls.stream().map(urlObserverFactory::getObserver).collect(Collectors.toList())
                )
        );
    }

    private void check(LandscapeObserverPool observerPool) {
        Optional<String> change = observerPool.hasChange();
        change.ifPresent(s -> {
            Landscape stored = observerPool.getLandscape();
            LandscapeDescription updated = landscapeDescriptionFactory.from(stored);
            LOGGER.info("Detected change '{}' in landscape {}", s, stored.getIdentifier());
            if (updated != null) {
                publisher.publishEvent(new IndexEvent(this, updated, "Source change: " + s));
            }
        });
    }
}