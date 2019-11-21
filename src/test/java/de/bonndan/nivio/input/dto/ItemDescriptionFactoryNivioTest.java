package de.bonndan.nivio.input.dto;


import de.bonndan.nivio.input.FileFetcher;
import de.bonndan.nivio.input.http.HttpService;
import de.bonndan.nivio.input.nivio.ItemDescriptionFactoryNivio;
import de.bonndan.nivio.model.*;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;


class ItemDescriptionFactoryNivioTest {

    private FileFetcher fileFetcher;

    private ItemDescriptionFactoryNivio descriptionFactory;

    @BeforeEach
    public void setup() {
        fileFetcher = new FileFetcher(new HttpService());
        descriptionFactory = new ItemDescriptionFactoryNivio(fileFetcher);
    }

    @Test
    public void readServiceAndInfra() {

        SourceReference file = new SourceReference(getRootPath() + "/src/test/resources/example/services/wordpress.yml");

        List<ItemDescription> services = descriptionFactory.getDescriptions(file, null);
        ItemDescription service = services.get(0);
        assertEquals(LandscapeItem.LAYER_APPLICATION, service.getLayer());
        assertEquals("Demo Blog", service.getName());
        assertEquals("to be replaced", service.getNote());
        assertEquals("blog-server", service.getIdentifier());
        assertEquals("blog", service.getShortName());
        assertEquals("1.0", service.getVersion());
        assertEquals("public", service.getVisibility());
        assertEquals("Wordpress", service.getSoftware());
        assertEquals("5", service.getScale());
        assertEquals("https://acme.io", service.getLinks().get("homepage").toString());
        assertEquals("https://git.acme.io/blog-server", service.getLinks().get("repository").toString());
        assertEquals("s", service.getMachine());
        assertNotNull(service.getNetworks());
        assertEquals("content", service.getNetworks().toArray()[0]);
        assertEquals("alphateam", service.getTeam());
        assertEquals("alphateam@acme.io", service.getContact());
        assertEquals("content", service.getGroup());
        assertEquals("docker", service.getHostType());
        assertEquals(1, service.getTags().length);
        assertTrue(Arrays.asList(service.getTags()).contains("CMS"));
        assertEquals(Lifecycle.END_OF_LIFE, service.getLifecycle());

        assertNotNull(service.getStatuses());
        assertEquals(3, service.getStatuses().size());
        service.getStatuses().forEach(statusItem -> {
            Assert.assertNotNull(statusItem);
            Assert.assertNotNull(statusItem.getLabel());
            if (statusItem.getLabel().equals(StatusItem.SECURITY)) {
                Assert.assertEquals(Status.RED, statusItem.getStatus());
            }
            if (statusItem.getLabel().equals(StatusItem.CAPABILITY)) {
                Assert.assertEquals(Status.YELLOW, statusItem.getStatus());
            }
        });

        assertNotNull(service.getInterfaces());
        assertEquals(3, service.getInterfaces().size());
        service.getInterfaces().forEach(dataFlow -> {
            if (dataFlow.getDescription().equals("posts")) {
                Assert.assertEquals("form", dataFlow.getFormat());
            }
        });

        assertNotNull(service.getRelations(RelationType.PROVIDER));
        assertEquals(3, service.getProvidedBy().size());

        Set<RelationItem<String>> dataflows = service.getRelations(RelationType.DATAFLOW);
        assertNotNull(dataflows);
        assertEquals(3, dataflows.size());
        dataflows.forEach(dataFlow -> {
             if (dataFlow.getDescription().equals("kpis")) {
                Assert.assertEquals("content-kpi-dashboard", dataFlow.getTarget());
            }
        });

        ItemDescription web = services.get(2);
        assertEquals(LandscapeItem.LAYER_INGRESS, web.getLayer());
        assertEquals("wordpress-web", web.getIdentifier());
        assertEquals("Webserver", web.getDescription());
        assertEquals("Apache", web.getSoftware());
        assertEquals("2.4", web.getVersion());
        assertEquals("Pentium 1 512MB RAM", web.getMachine());
        assertEquals("ops guys", web.getTeam());
        assertEquals("content", web.getNetworks().toArray()[0]);
        assertEquals("docker", web.getHostType());
    }

    @Test
    public void readIngress() {

        SourceReference file = new SourceReference(getRootPath() + "/src/test/resources/example/services/dashboard.yml");

        List<ItemDescription> services = descriptionFactory.getDescriptions(file, null);
        ItemDescription service = services.get(0);
        assertEquals(LandscapeItem.LAYER_INGRESS, service.getGroup());
        assertEquals("Keycloak SSO", service.getName());
        assertEquals("keycloak", service.getIdentifier());
    }

    private String getRootPath() {
        Path currentRelativePath = Paths.get("");
        return currentRelativePath.toAbsolutePath().toString();
    }
}