package net.oneandone.lavender.filter.processor;

import java.util.function.Predicate;
/**
 * An enum to track the current attribute.
 */
public enum LavenderHtmlAttribute implements HtmlAttribute {

    SRC("src"),
    HREF("href"),
    REL("rel"),
    STYLE("style"),
    TYPE("type"),
    NAME("name"),
    VALUE("value"),
    ACTION("action"),

    /** everything starting with "data-lavender-" */
    DATA_LAVENDER_ATTR(x -> x.startsWith("data-lavender-"));

    private Predicate<String> attributeMatch;

    LavenderHtmlAttribute(Predicate<String> attributeMatch) {
        this.attributeMatch = attributeMatch;
    }

    LavenderHtmlAttribute(String attributeName) {
        this.attributeMatch = x -> attributeName.equalsIgnoreCase(x);
    }

    @Override
    public boolean attributeMatches(String attributeName) {
        return attributeMatch.test(attributeName);
    }
}
