package de.bonndan.nivio.input.compose2;

import de.bonndan.nivio.input.FileFetcher;
import de.bonndan.nivio.input.http.HttpService;
import de.bonndan.nivio.input.dto.ItemDescription;
import de.bonndan.nivio.input.dto.SourceReference;
import de.bonndan.nivio.model.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InputFormatHandlerCompose2Test {

    private FileFetcher fileFetcher;

    @BeforeEach
    public void setup() {
        fileFetcher = new FileFetcher(new HttpService());
    }

    @Test
    public void readCompose() {
        SourceReference file = new SourceReference(new File(getRootPath() + "/src/test/resources/example/services/docker-compose.yml").toURI().toString());
        String yml = fileFetcher.get(file);
        InputFormatHandlerCompose2 factoryCompose2 = new InputFormatHandlerCompose2(fileFetcher);
        List<ItemDescription> services = factoryCompose2.getDescriptions(file, null);
        assertEquals(3, services.size());
        ItemDescription service = services.get(0);
        assertNotNull(service);

        assertEquals("web", service.getIdentifier());
        assertNotNull(service.getLabels(Label.network));
        assertEquals(2, service.getLabels(Label.network).size());
    }

    private String getRootPath() {
        Path currentRelativePath = Paths.get("");
        return currentRelativePath.toAbsolutePath().toString();
    }
}
