/*
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

import java.util.function.Predicate;
/**
 * An enum to track the current attribute.
 */
public enum LavenderHtmlAttribute implements HtmlAttribute {

    SRC("src"),
    SRCSET("srcset"),
    HREF("href"),
    REL("rel"),
    STYLE("style"),
    TYPE("type"),
    NAME("name"),
    VALUE("value"),
    ACTION("action"),
    DATA("data"),
    PROPERTY("property"),
    CONTENT("content"),

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
