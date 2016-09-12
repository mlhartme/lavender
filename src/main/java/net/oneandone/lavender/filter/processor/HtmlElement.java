/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
