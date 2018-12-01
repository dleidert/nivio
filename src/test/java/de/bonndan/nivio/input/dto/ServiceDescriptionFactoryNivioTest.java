package de.bonndan.nivio.input.dto;


import de.bonndan.nivio.input.FileFetcher;
import de.bonndan.nivio.input.HttpService;
import de.bonndan.nivio.input.nivio.ServiceDescriptionFactoryNivio;
import de.bonndan.nivio.landscape.LandscapeItem;
import de.bonndan.nivio.landscape.Status;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;


class ServiceDescriptionFactoryNivioTest {

    private FileFetcher fileFetcher;

    private ServiceDescriptionFactoryNivio descriptionFactory;

    @BeforeEach
    public void setup() {
        fileFetcher = new FileFetcher(new HttpService());
        descriptionFactory = new ServiceDescriptionFactoryNivio();
    }

    @Test
    public void readServiceAndInfra() {

        SourceReference file = new SourceReference(getRootPath() + "/src/test/resources/example/services/wordpress.yml");
        String yml = fileFetcher.get(file);
        List<ServiceDescription> services = descriptionFactory.fromString(yml);
        ServiceDescription service = services.get(0);
        assertEquals(LandscapeItem.LAYER_APPLICATION, service.getLayer());
        assertEquals("Demo Blog", service.getName());
        assertEquals("to be replaced", service.getNote());
        assertEquals("blog-server", service.getIdentifier());
        assertEquals("blog", service.getShort_name());
        assertEquals("1.0", service.getVersion());
        assertEquals("public", service.getVisibility());
        assertEquals("Wordpress", service.getSoftware());
        assertEquals("5", service.getScale());
        assertEquals("https://acme.io", service.getHomepage());
        assertEquals("https://git.acme.io/blog-server", service.getRepository());
        assertEquals("s", service.getMachine());
        assertNotNull(service.getNetworks());
        assertEquals("content", service.getNetworks().toArray()[0]);
        assertEquals("alphateam", service.getTeam());
        assertEquals("alphateam@acme.io", service.getContact());
        assertEquals("content", service.getGroup());
        assertEquals("docker", service.getHost_type());
        assertEquals(1, service.getTags().length);
        assertTrue(Arrays.asList(service.getTags()).contains("CMS"));

        assertNotNull(service.getStatuses());
        assertEquals(4, service.getStatuses().size());
        service.getStatuses().forEach((status, color) -> {
            if (status.equals(LandscapeItem.STATUS_KEY_SECURITY)) {
                Assert.assertEquals(Status.RED, color);
            }
            if (status.equals(LandscapeItem.STATUS_KEY_BUSINESS_CAPABILITY)) {
                Assert.assertEquals(Status.YELLOW, color);
            }
        });

        assertNotNull(service.getInterfaces());
        assertEquals(3, service.getInterfaces().size());
        service.getInterfaces().forEach(dataFlow -> {
            if (dataFlow.getDescription().equals("posts")) {
                Assert.assertEquals("form", dataFlow.getFormat());
            }
        });

        assertNotNull(service.getDataFlow());
        assertEquals(2, service.getDataFlow().size());
        service.getDataFlow().forEach(dataFlow -> {
            if (dataFlow.getDescription().equals("kpis")) {
                Assert.assertEquals("content-kpi-dashboard", dataFlow.getTarget());
            }
        });

        ServiceDescription infra = services.get(1);
        assertEquals(LandscapeItem.LAYER_INFRASTRUCTURE, infra.getLayer());
        assertEquals("wordpress-web", infra.getIdentifier());
        assertEquals("Webserver", infra.getDescription());
        assertEquals("Apache", infra.getSoftware());
        assertEquals("2.4", infra.getVersion());
        assertEquals("Pentium 1 512MB RAM", infra.getMachine());
        assertEquals("ops guys", infra.getTeam());
        assertEquals("content", infra.getNetworks().toArray()[0]);
        assertEquals("docker", infra.getHost_type());
    }

    @Test
    public void readIngress() {

        SourceReference file = new SourceReference(getRootPath() + "/src/test/resources/example/services/dashboard.yml");
        String yml = fileFetcher.get(file);


        List<ServiceDescription> services = descriptionFactory.fromString(yml);
        ServiceDescription service = services.get(0);
        assertEquals(LandscapeItem.LAYER_INGRESS, service.getLayer());
        assertEquals("Keycloak SSO", service.getName());
        assertEquals("keycloak", service.getIdentifier());
    }

    private String getRootPath() {
        Path currentRelativePath = Paths.get("");
        return currentRelativePath.toAbsolutePath().toString();
    }
}