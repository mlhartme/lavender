package net.oneandone.lavender.filter.processor;

public interface UrlRewriteMatcher {

    boolean matches(HtmlElement htmlElement);

    HtmlAttribute getAttributeToRewrite();

}
