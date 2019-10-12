package de.bonndan.nivio.input;

import de.bonndan.nivio.input.dto.LandscapeDescription;
import de.bonndan.nivio.input.dto.ItemDescription;
import de.bonndan.nivio.model.*;
import de.bonndan.nivio.notification.NotificationService;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;

import static de.bonndan.nivio.model.ServiceItems.pick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IndexerIntegrationTest {

    @Autowired
    LandscapeRepository landscapeRepository;

    @Mock
    NotificationService notificationService;

    @MockBean
    JavaMailSender mailSender;

    private LandscapeImpl index() {
        return index("/src/test/resources/example/example_env.yml");
    }

    private LandscapeImpl index(String path) {
        File file = new File(getRootPath() + path);
        LandscapeDescription landscapeDescription = EnvironmentFactory.fromYaml(file);

        Indexer indexer = new Indexer(landscapeRepository, notificationService);

        ProcessLog processLog = indexer.reIndex(landscapeDescription);
        return (LandscapeImpl) processLog.getLandscape();
    }

    @Test //first pass
    public void testIndexing() {
        LandscapeImpl landscape = index();

        Assertions.assertNotNull(landscape);
        assertEquals("mail@acme.org", landscape.getContact());
        Assertions.assertNotNull(landscape.getItems());
        assertEquals(8, landscape.getItems().size());
        Item blog = (Item) ServiceItems.pick("blog-server", null, landscape.getItems());
        Assertions.assertNotNull(blog);
        assertEquals(3, blog.getProvidedBy().size());

        Item webserver = (Item) ServiceItems.pick("wordpress-web", null, new ArrayList<>(blog.getProvidedBy()));
        Assertions.assertNotNull(webserver);
        assertEquals(1, webserver.getProvides().size());

        DataFlow push = (DataFlow) blog.getDataFlow().stream()
                .filter(d -> d.getDescription().equals("push"))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(push);

        assertEquals("push", push.getDescription());
        assertEquals("json", push.getFormat());
        assertEquals(blog.getIdentifier(), push.getSourceEntity().getIdentifier());
        assertEquals("nivio:example/dashboard/kpi-dashboard", push.getTarget());

        Set<InterfaceItem> interfaces = blog.getInterfaces();
        assertEquals(3, interfaces.size());
        InterfaceItem i =  blog.getInterfaces().stream()
                .filter(d -> d.getDescription().equals("posts"))
                .findFirst()
                .orElseThrow();
        assertEquals("form", i.getFormat());
        assertEquals("http://acme.io/create", i.getUrl().toString());
    }

    @Test //second pass
    public void testReIndexing() {
        LandscapeImpl landscape = index();

        Assertions.assertNotNull(landscape);
        assertEquals("mail@acme.org", landscape.getContact());
        Assertions.assertNotNull(landscape.getItems());
        assertEquals(8, landscape.getItems().size());
        Item blog = (Item) ServiceItems.pick("blog-server", null,landscape.getItems());
        Assertions.assertNotNull(blog);
        assertEquals(3, blog.getProvidedBy().size());

        Item webserver = (Item) ServiceItems.pick("wordpress-web", null, new ArrayList<LandscapeItem>(blog.getProvidedBy()));
        Assertions.assertNotNull(webserver);
        assertEquals(1, webserver.getProvides().size());

        DataFlow push = (DataFlow) blog.getDataFlow().stream()
                .filter(d -> d.getDescription().equals("push"))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(push);

        assertEquals("push", push.getDescription());
        assertEquals("json", push.getFormat());
        assertEquals(blog.getIdentifier(), push.getSourceEntity().getIdentifier());
        assertEquals("nivio:example/dashboard/kpi-dashboard", push.getTarget());

        Set<InterfaceItem> interfaces = blog.getInterfaces();
        assertEquals(3, interfaces.size());
        InterfaceItem i =  blog.getInterfaces().stream()
                .filter(d -> d.getDescription().equals("posts"))
                .findFirst()
                .orElseThrow();
        assertEquals("form", i.getFormat());
    }

    /**
     * wordpress-web updates must not create new services
     */
    @Test
    public void testIncrementalUpdate() {
        LandscapeImpl landscape = index();
        Item blog = (Item) ServiceItems.pick("blog-server", null, landscape.getItems());
        int before = landscape.getItems().size();

        LandscapeDescription landscapeDescription = new LandscapeDescription();
        landscapeDescription.setIdentifier(landscape.getIdentifier());
        landscapeDescription.setIsPartial(true);

        ItemDescription newItem = new ItemDescription();
        newItem.setIdentifier(blog.getIdentifier());
        newItem.setGroup("completelyNewGroup");
        landscapeDescription.getItemDescriptions().add(newItem);

        ItemDescription exsistingWordPress = new ItemDescription();
        exsistingWordPress.setIdentifier("wordpress-web");
        exsistingWordPress.setName("Other name");
        landscapeDescription.getItemDescriptions().add(exsistingWordPress);

        Indexer indexer = new Indexer(landscapeRepository, notificationService);

        //created
        landscape = (LandscapeImpl) indexer.reIndex(landscapeDescription).getLandscape();
        blog = (Item) ServiceItems.pick("blog-server", "completelyNewGroup", landscape.getItems());
        assertEquals("completelyNewGroup", blog.getGroup());
        assertEquals(before +1, landscape.getItems().size());

        //updated
        Item wordpress = (Item) ServiceItems.pick("wordpress-web", "content", landscape.getItems());
        assertEquals("Other name", wordpress.getName());
        assertEquals("content", wordpress.getGroup());


    }

    /**
     * Ensures that same names in different landscapes do not collide
     */
    @Test
    public void testNameConflictDifferentLandscapes() {
        LandscapeImpl landscape1 = index("/src/test/resources/example/example_env.yml");
        LandscapeImpl landscape2 = index("/src/test/resources/example/example_other.yml");

        Assertions.assertNotNull(landscape1);
        assertEquals("mail@acme.org", landscape1.getContact());
        Assertions.assertNotNull(landscape1.getItems());
        Item blog1 = (Item) ServiceItems.pick("blog-server", null,landscape1.getItems());
        Assertions.assertNotNull(blog1);
        assertEquals("blog", blog1.getShortName());

        Assertions.assertNotNull(landscape2);
        assertEquals("nivio:other", landscape2.getIdentifier());
        assertEquals("mail@other.org", landscape2.getContact());
        Assertions.assertNotNull(landscape2.getItems());
        Item blog2 = (Item) ServiceItems.pick("blog-server", null,landscape2.getItems());
        Assertions.assertNotNull(blog2);
        assertEquals("blog1", blog2.getShortName());
    }

    /**
     * Ensures that same names in different landscapes do not collide
     */
    @Test
    public void testDataflow() {
        LandscapeImpl landscape1 = index("/src/test/resources/example/example_dataflow.yml");

        Assertions.assertNotNull(landscape1);
        Assertions.assertNotNull(landscape1.getItems());
        Item blog1 = (Item) ServiceItems.pick("blog-server", "content1",landscape1.getItems());
        Assertions.assertNotNull(blog1);
        Item blog2 = (Item) ServiceItems.pick("blog-server", "content2",landscape1.getItems());
        Assertions.assertNotNull(blog2);
        assertEquals("Demo Blog", blog1.getName());
        assertEquals(
                FullyQualifiedIdentifier.build("nivio:dataflowtest", "content1", "blog-server").toString(),
                blog1.toString()
        );

        assertNotNull(blog1.getDataFlow());
        assertEquals(1, blog1.getDataFlow().size());
    }

    @Test
    public void environmentTemplatesApplied() {
        LandscapeImpl landscape = index("/src/test/resources/example/example_templates.yml");

        LandscapeItem web = pick( "web", null, landscape.getItems());
        Assert.assertNotNull(web);
        assertEquals("web", web.getIdentifier());
        assertEquals("webservice", web.getType());
    }

    private String getRootPath() {
        Path currentRelativePath = Paths.get("");
        return currentRelativePath.toAbsolutePath().toString();
    }
}
