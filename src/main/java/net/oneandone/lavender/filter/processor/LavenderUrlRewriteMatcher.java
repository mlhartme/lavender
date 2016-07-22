package net.oneandone.lavender.filter.processor;

import java.util.Arrays;
import java.util.function.Predicate;

import static net.oneandone.lavender.filter.processor.LavenderHtmlAttribute.ACTION;
import static net.oneandone.lavender.filter.processor.LavenderHtmlAttribute.DATA_LAVENDER_ATTR;
import static net.oneandone.lavender.filter.processor.LavenderHtmlAttribute.HREF;
import static net.oneandone.lavender.filter.processor.LavenderHtmlAttribute.REL;
import static net.oneandone.lavender.filter.processor.LavenderHtmlAttribute.SRC;
import static net.oneandone.lavender.filter.processor.LavenderHtmlAttribute.TYPE;
import static net.oneandone.lavender.filter.processor.LavenderHtmlTag.A;
import static net.oneandone.lavender.filter.processor.LavenderHtmlTag.FORM;
import static net.oneandone.lavender.filter.processor.LavenderHtmlTag.IFRAME;
import static net.oneandone.lavender.filter.processor.LavenderHtmlTag.IMG;
import static net.oneandone.lavender.filter.processor.LavenderHtmlTag.INPUT;
import static net.oneandone.lavender.filter.processor.LavenderHtmlTag.LINK;
import static net.oneandone.lavender.filter.processor.LavenderHtmlTag.SCRIPT;
import static net.oneandone.lavender.filter.processor.LavenderHtmlTag.SOURCE;

public enum LavenderUrlRewriteMatcher implements UrlRewriteMatcher {

    IMG_MATCHER(SRC, p -> p.getTag() == IMG),
    LINK_MATCHER(HREF, p -> p.getTag() == LINK &&
            Arrays.asList("stylesheet", "icon", "shortcut icon").contains(p.getAttribute(REL))),
    SCRIPT_MATCHER(SRC, p -> p.getTag() == SCRIPT &&
            (("text/javascript".equals(p.getAttribute(TYPE))) || !p.containsAttribute(TYPE))),
    INPUT_MATCHER(SRC, p -> p.getTag() == INPUT && "image".equals(p.getAttribute(TYPE))),
    A_MATCHER(HREF, p -> p.getTag() == A),
    SOURCE_MATCHER(SRC, p -> p.getTag() == SOURCE),
    FORM_MATCHER(ACTION, p -> p.getTag() == FORM),
    IFRAME_MATCHER(SRC, p -> p.getTag() == IFRAME),
    DATA_LAVENDER_MATCHER(DATA_LAVENDER_ATTR, p -> true);


    private final Predicate<HtmlElement> predicate;
    private final HtmlAttribute attributeToRewrite;

    LavenderUrlRewriteMatcher(HtmlAttribute attributeToRewrite, Predicate<HtmlElement> rewritePredicate) {
        this.attributeToRewrite = attributeToRewrite;
        this.predicate = rewritePredicate;
    }

    @Override
    public boolean matches(HtmlElement htmlElement) {
        return predicate.test(htmlElement);
    }

    @Override
    public HtmlAttribute getAttributeToRewrite() {
        return attributeToRewrite;
    }

}
