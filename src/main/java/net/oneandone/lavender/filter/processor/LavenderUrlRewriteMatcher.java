package net.oneandone.lavender.filter.processor;

import static net.oneandone.lavender.filter.processor.LavenderHtmlAttribute.ACTION;
import static net.oneandone.lavender.filter.processor.LavenderHtmlAttribute.DATA_LAVENDER_ATTR;
import static net.oneandone.lavender.filter.processor.LavenderHtmlAttribute.HREF;
import static net.oneandone.lavender.filter.processor.LavenderHtmlAttribute.REL;
import static net.oneandone.lavender.filter.processor.LavenderHtmlAttribute.SRC;
import static net.oneandone.lavender.filter.processor.LavenderHtmlAttribute.TYPE;
import static net.oneandone.lavender.filter.processor.LavendertHtmlTag.A;
import static net.oneandone.lavender.filter.processor.LavendertHtmlTag.FORM;
import static net.oneandone.lavender.filter.processor.LavendertHtmlTag.IFRAME;
import static net.oneandone.lavender.filter.processor.LavendertHtmlTag.IMG;
import static net.oneandone.lavender.filter.processor.LavendertHtmlTag.INPUT;
import static net.oneandone.lavender.filter.processor.LavendertHtmlTag.LINK;
import static net.oneandone.lavender.filter.processor.LavendertHtmlTag.SCRIPT;
import static net.oneandone.lavender.filter.processor.LavendertHtmlTag.SOURCE;

public enum LavenderUrlRewriteMatcher implements UrlRewriteMatcher {

    IMG_MATCHER(IMG, SRC),
    LINK_MATCHER(LINK, HREF, new HtmlAttributeValue(REL, "stylesheet", "icon", "shortcut icon")),
    SCRIPT_MATCHER(SCRIPT, SRC, new HtmlAttributeValue(TYPE, "text/javascript")),
    INPUT_MATCHER(INPUT, SRC, new HtmlAttributeValue(TYPE, "image")),
    A_MATCHER(A, HREF),
    SOURCE_MATCHER(SOURCE, SRC),
    FORM_MATCHER(FORM, ACTION),
    IFRAME_MATCHER(IFRAME, SRC),
    DATA_LAVENDER_MATCHER(DATA_LAVENDER_ATTR);

    private HtmlTag rewriteTag;
    private HtmlAttribute rewriteAttribute;
    private HtmlAttributeValue attributeTypeAndValueToMatch;

    LavenderUrlRewriteMatcher(HtmlAttribute rewriteAttribute) {
        this(null, rewriteAttribute, null);
    }

    LavenderUrlRewriteMatcher(HtmlTag rewriteTag, HtmlAttribute rewriteAttribute) {
        this(rewriteTag, rewriteAttribute, null);
    }

    LavenderUrlRewriteMatcher(HtmlTag rewriteTag, HtmlAttribute rewriteAttribute,
            HtmlAttributeValue attributeTypeNValueToMatch) {
        this.rewriteTag = rewriteTag;
        this.rewriteAttribute = rewriteAttribute;
        this.attributeTypeAndValueToMatch = attributeTypeNValueToMatch;
    }

    @Override
    public HtmlAttribute getAttributeToMatch() {
        if (attributeTypeAndValueToMatch != null) {
            return attributeTypeAndValueToMatch.getAttribute();
        }
        return null;
    }


    @Override
    public boolean matches(HtmlTag rewriteTag, HtmlAttribute rewriteAttribute, HtmlAttribute matchingAttributeType,
            String matchingAttributeValue) {
        if (rewriteTagMatches(this.rewriteTag, rewriteTag) && rewriteAttributeMatches(this.rewriteAttribute,
                rewriteAttribute) && attributeTypeAndAttributeValueMatches(attributeTypeAndValueToMatch,
                matchingAttributeType, matchingAttributeValue)) {
            return true;
        }

        return false;
    }

    private boolean rewriteTagMatches(HtmlTag ownRewriteTag, HtmlTag otherRewriteTag) {
        return ownRewriteTag == null || ownRewriteTag.equals(otherRewriteTag);
    }

    private boolean rewriteAttributeMatches(HtmlAttribute ownRewriteAttribute, HtmlAttribute otherRewriteAttribute) {
        return ownRewriteAttribute.equals(otherRewriteAttribute);
    }

    private boolean attributeTypeAndAttributeValueMatches(HtmlAttributeValue attributeTypeAndValueToMatch,
            HtmlAttribute matchingAttributeType, String matchingAttributeValue) {
        if (matchingAttributeType == null && matchingAttributeValue == null) {
            return true;
        } else {
            if (attributeTypeAndValueToMatch.getAttribute().equals(matchingAttributeType)
                    && attributeTypeAndValueToMatch.containsAttributeValue(matchingAttributeValue)) {
                return true;
            }
        }
        return false;
    }
}
