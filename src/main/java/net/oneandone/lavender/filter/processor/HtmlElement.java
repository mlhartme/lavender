package net.oneandone.lavender.filter.processor;

import net.oneandone.lavender.filter.processor.HtmlProcessor.HtmlAttributeValue;

import java.util.List;

public class HtmlElement {
    private HtmlTag tag;
    private List<HtmlAttributeValue> attributeValues;

    public HtmlElement(HtmlTag tag, List<HtmlAttributeValue> attributeValues) {
        this.tag = tag;
        this.attributeValues = attributeValues;
    }

    public HtmlTag getTag() {
        return tag;
    }

    public boolean containsAttribute(HtmlAttribute attribute) {
        for (HtmlAttributeValue attributeValue : attributeValues) {
            if (attributeValue.getAttribute() == attribute){
                return true;
            }
        }
        return false;
    }

    public String getAttribute(HtmlAttribute attribute) {
        for (HtmlAttributeValue attr : attributeValues) {
            if (attr.getAttribute() == attribute){
                return attr.getValue();
            }
        }
        return null;
    }
}
