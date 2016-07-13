package net.oneandone.lavender.filter.processor;

public interface UrlRewriteMatcher {
    boolean matches(HtmlTag htmlTag, HtmlAttribute rewriteAttribute, HtmlAttribute matchingAttributeType,
            String matchingAttributeValue);
    HtmlAttribute getAttributeToMatch();
}
