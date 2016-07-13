package net.oneandone.lavender.filter.processor;

/**
 * An enum to track the current tag.
 */
public enum LavendertHtmlTag implements HtmlTag {

    NULL(""),
    IMG("img"),
    LINK("link"),
    SCRIPT("script"),
    INPUT("input"),
    A("a"),
    SOURCE("source"),
    FORM("form"),
    IFRAME("iframe"),
    OTHER("");

    private String name;

    LavendertHtmlTag(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
