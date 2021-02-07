package de.bonndan.nivio.model;

import org.junit.jupiter.api.Test;

import static de.bonndan.nivio.model.ItemFactory.getTestItem;
import static org.junit.jupiter.api.Assertions.*;

public class ItemTest {

    @Test
    public void equalsWithGroup() {

        Landscape landscape = LandscapeFactory.createForTesting("l1", "l1Landscape").build();


        Item s1 = getTestItem("g1", "a", landscape);

        Item s2 = getTestItem("g1", "a", landscape);

        Item s3 = getTestItem("g2", "a", landscape);

        Item s4 = getTestItem(Group.COMMON, "a", landscape);

        assertEquals(s1, s2);
        assertEquals(s2, s1);
        assertNotEquals(s3, s1);
        assertNotEquals(s3, s2);
        assertNotEquals(s4, s1);
        assertNotEquals(s4, s2);
    }

    @Test
    public void equalsWithLandscape() {

        Landscape landscape = LandscapeFactory.createForTesting("l1", "l1Landscape").build();

        Item s1 = getTestItem("g1", "a", landscape);

        Item s2 = getTestItem("g1", "a", landscape);

        assertEquals(s1, s2);

        Item s3 = getTestItem("g1", "a");

        assertNotEquals(s1, s3);
    }
}
