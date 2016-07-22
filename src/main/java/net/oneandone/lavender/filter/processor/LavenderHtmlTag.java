package net.oneandone.lavender.filter.processor;

/**
 * An enum to track the current currentTag.
 */
public enum LavenderHtmlTag implements HtmlTag {

    IMG("img"),
    LINK("link"),
    SCRIPT("script"),
    INPUT("input"),
    A("a"),
    SOURCE("source"),
    FORM("form"),
    IFRAME("iframe");

    private String name;

    LavenderHtmlTag(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
