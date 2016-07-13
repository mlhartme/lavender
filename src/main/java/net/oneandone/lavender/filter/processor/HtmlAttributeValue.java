package net.oneandone.lavender.filter.processor;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class HtmlAttributeValue {
    private HtmlAttribute attribute;
    private Set<String> values;

    public HtmlAttributeValue(HtmlAttribute attribute, String... values) {
        this.attribute = attribute;
        this.values = new HashSet<String>(asList(values));
    }

    public HtmlAttribute getAttribute() {
        return attribute;
    }

    public boolean containsAttributeValue(String value){
        return values.contains(value);
    }
}
