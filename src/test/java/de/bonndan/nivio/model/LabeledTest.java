package de.bonndan.nivio.model;

import de.bonndan.nivio.input.dto.ItemDescription;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class LabeledTest {

    @Test
    void groupedByPrefixes() {
        Map<String, String> all = new HashMap<>();
        all.put(Label.costs.name(), "0");
        all.put(Label.note.name(), "foo");
        all.put(Label.network + Label.DELIMITER + "x", "x");
        all.put(Label.network + Label.DELIMITER + "y", "y");
        all.put(Label.network + Label.DELIMITER + "z", "z");

        Map<String, String> groupedByPrefixes = Labeled.groupedByPrefixes(all);
        assertNotNull(groupedByPrefixes);
        assertEquals(3, groupedByPrefixes.size());
        assertEquals("0", groupedByPrefixes.get(Label.costs.name()));
        assertEquals("foo", groupedByPrefixes.get(Label.note.name()));
        assertTrue(groupedByPrefixes.get(Label.network.name()).contains("x"));
        assertTrue(groupedByPrefixes.get(Label.network.name()).contains("y"));
        assertTrue(groupedByPrefixes.get(Label.network.name()).contains("z"));
    }

    @Test
    public void setPrefixed() {
        ItemDescription itemDescription = new ItemDescription();
        itemDescription.setPrefixed(Label.network, "foo");
        assertEquals(1, itemDescription.getLabels(Label.network).size());
        assertTrue(itemDescription.getLabels().containsKey(Label.network + ".foo"));
        String label = itemDescription.getLabel(Label.network + ".foo");
        assertEquals("foo", label);
    }

    @Test
    public void setLabelsDoesNotOverwriteExistingOnes() {
        ItemDescription itemDescription = new ItemDescription();
        itemDescription.setLabel("foo", "bar");

        //when
        itemDescription.setLabels(Map.of("one", "two"));

        assertEquals(2, itemDescription.getLabels().size());
        assertTrue(itemDescription.getLabels().containsKey( "foo"));
    }

    @Test
    public void withoutPrefixes() {
        ItemDescription itemDescription = new ItemDescription();
        itemDescription.setLabel(Label.costs, "123");
        itemDescription.setPrefixed(Label.network, "foo");
        itemDescription.setPrefixed(Label.status, "bar");
        itemDescription.setPrefixed(Label.condition, "bar");

        Map<String, String> stringStringMap = Labeled.withoutKeys(itemDescription.getLabels(), Label.condition.name(), Label.status.name());
        assertThat(stringStringMap).isNotNull();
        assertThat(stringStringMap.size()).isEqualTo(2);
        assertThat(stringStringMap.get(Label.costs.name())).isEqualTo("123");
        assertThat(stringStringMap.get(Label.network.name()+".foo")).isEqualTo("foo");
    }
}