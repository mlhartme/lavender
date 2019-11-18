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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlProcessor extends AbstractProcessor {

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(HtmlProcessor.class);

    private static final HtmlTag OTHER_HTML_TAG = () -> "";
    private static final HtmlAttribute OTHER_HTML_ATTRIBUTE = x -> false;

    /**
     * Parse URLs from image candidate strings in "srcset" according to https://html.spec.whatwg.org/multipage/images.html#srcset-attributes
     * Matching groups:
     * <ul>
     *     <li>ASCII Whitespace (optional</li>
     *     <li>URL</li>
     *     <li>ASCII Whitespace (optional)</li>
     *     <li>Remainder (Width or Density descriptors) including ASCII Whitespace (optional)</li>
     *     <li>Candidate separator (',') or end of line</li>
     * </ul>
     */
    private static final Pattern SRCSET_PATTERN = Pattern.compile("([\\t\\n\\f\\r ]*)([^,\\t\\n\\f\\r ]+)([\\t\\n\\f\\r ]*)([^,]*)(,|$)");

    /** The main state of this processor. */
    protected State state = State.NULL;

    /**
     * The current currentTag, but only if it has attributes. CAUTION: properly set only between &lt; ... &gt;;
     * outside of angle brackets, it contains the last value of currentTag.
     */
    protected HtmlTag currentTag;

    /** The current attribute within a currentTag. */
    protected HtmlAttribute currentAttribute;
    private int attributeValueStartIndex = -1;
    private int attributeNameStartIndex = -1;

    /** The currentTag buffer. */
    protected StringBuilder tagBuffer = new StringBuilder(100);

    /** The relevant attributes in the current currentTag, in order. */
    protected List<HtmlAttributeValue> attributes = new ArrayList<>();

    private HtmlTag[] knownTags;
    private HtmlAttribute[] knownAttributes;
    private UrlRewriteMatcher[] urlRewriteMatchers;


    /**
     * An enum to track the state of this processor.
     */
    enum State {
        NULL,

        //
        SPECIAL_START,
        SPECIAL_START_COMMENT_OR_CONDITION,
        SPECIAL_DOCTYPE,
        SPECIAL_CDATA,
        SPECIAL_COMMENT,

        //
        TAG_START,
        TAG,
        ATTRIBUTE_START,
        ATTRIBUTE,
        ATTRIBUTE_EQUALS,
        VALUE_START_SQ,
        VALUE_START_DQ,
        VALUE_START_UQ,
        VALUE
    }

    static final class HtmlAttributeValue {
        private final HtmlAttribute attr;
        private final int start;
        private final int end;
        private final StringBuilder tagBuffer;

        private HtmlAttributeValue(HtmlAttribute attr, int start, int end, StringBuilder tagBuffer) {
            this.attr = attr;
            this.start = start;
            this.end = end;
            this.tagBuffer = tagBuffer;
        }

        public HtmlAttribute getAttribute() {
            return attr;
        }

        public String getValue() {
            return tagBuffer.substring(start, end);
        }
    }


    /**
     * Instantiates a new HTML processor.
     */
    public HtmlProcessor() {
        this(LavenderHtmlTag.values(), LavenderHtmlAttribute.values(), LavenderUrlRewriteMatcher.values());
    }

    /**
     * Instantiates a new HTML processor.
     */
    public HtmlProcessor(HtmlTag[] knownTags, HtmlAttribute[] knownAttributes, UrlRewriteMatcher[] urlRewriteMatchers) {
        super(LOG);
        this.knownTags = knownTags;
        this.knownAttributes = knownAttributes;
        this.urlRewriteMatchers = urlRewriteMatchers;
    }

    @Override
    public void flush() throws IOException {
        if (tagBuffer.length() > 0) {
            out.write(tagBuffer.toString());
        }
        super.flush();
    }

    /**
     * {@inheritDoc}
     */
    protected void process(char c) throws IOException {

        switch (state) {
            case NULL:
                matchTagStart(c);
                break;
            case SPECIAL_START:
                matchSpecialStart(c);
                break;
            case SPECIAL_START_COMMENT_OR_CONDITION:
                matchSpecialStartCommentOrCondition(c);
                break;
            case SPECIAL_DOCTYPE:
            case SPECIAL_COMMENT:
            case SPECIAL_CDATA:
                matchSpecialEnd(c);
                break;
            case TAG_START:
                matchTag(c);
                break;
            case TAG:
                matchInTag(c);
                break;
            case ATTRIBUTE_START:
                matchAttribute(c);
                break;
            case ATTRIBUTE:
                matchInAttribute(c);
                break;
            case ATTRIBUTE_EQUALS:
                matchValueStart(c);
                break;
            case VALUE_START_DQ:
                matchDoubleQuotedValue(c);
                break;
            case VALUE_START_SQ:
                matchSingleQuotedValue(c);
                break;
            case VALUE_START_UQ:
                matchUnquotedValue(c);
                break;

            default:
                throw new IllegalStateException("Unexpected state: " + state);
        }
    }

    private void matchSpecialStart(char c) throws IOException {
        String t = tagBuffer.append(c).toString();
        if (t.equalsIgnoreCase("--")) {
            state = State.SPECIAL_START_COMMENT_OR_CONDITION;
        } else if (t.equalsIgnoreCase("[if") || t.equalsIgnoreCase("[endif")) {
            state = State.NULL;
            tagBuffer.setLength(0);
        } else if (t.equalsIgnoreCase("DOCTYPE")) {
            state = State.SPECIAL_DOCTYPE;
            tagBuffer.setLength(0);
        } else if (t.equalsIgnoreCase("[CDATA[")) {
            state = State.SPECIAL_CDATA;
            tagBuffer.setLength(0);
        }

        out.write(c);
    }

    private void matchSpecialStartCommentOrCondition(char c) throws IOException {
        if (c == '[') {
            // condition
            state = State.NULL;
        } else if (c == '>') {
            // end of comment
            state = State.NULL;
        } else {
            state = State.SPECIAL_COMMENT;
        }

        tagBuffer.setLength(0);

        out.write(c);
    }

    private void matchSpecialEnd(char c) throws IOException {
        switch (state) {

            case SPECIAL_DOCTYPE:
                if (c == '>') {
                    state = State.NULL;
                }
                break;

            case SPECIAL_COMMENT:
                if (c == '-') {
                    tagBuffer.append(c);
                } else if (c == '>') {
                    if (tagBuffer.toString().endsWith("--")) {
                        state = State.NULL;
                    }
                    tagBuffer.setLength(0);
                } else {
                    tagBuffer.setLength(0);
                }
                break;

            case SPECIAL_CDATA:
                if (c == ']') {
                    tagBuffer.append(c);
                } else if (c == '>') {
                    if (tagBuffer.toString().endsWith("]]")) {
                        state = State.NULL;
                    }
                    tagBuffer.setLength(0);
                } else {
                    tagBuffer.setLength(0);
                }
                break;

            default:
                throw new IllegalStateException("" + state);
        }

        out.write(c);
    }

    private void matchTagStart(char c) throws IOException {
        if (c == '<') {
            state = State.TAG_START;
        }

        out.write(c);
    }

    private void matchTag(char c) throws IOException {
        if (Character.isSpaceChar(c)) {
            state = State.TAG;
            currentTag = findTagByName(tagBuffer.toString().toLowerCase());
            tagBuffer.append(c);
        } else if (c == '>') {
            processTagBuffer();
            state = State.NULL;
            tagBuffer.setLength(0);
            out.write(c);
        } else if (c == '!') {
            // comment
            state = State.SPECIAL_START;
            out.write(c);
        } else {
            // still in currentTag
            tagBuffer.append(c);
        }
    }

    private void matchInTag(char c) throws IOException {
        if (c == '>') {
            processTagBuffer();
            state = State.NULL;
            tagBuffer.setLength(0);
            out.write(c);
        } else if (c == '/') {
            // ignore this
            tagBuffer.append(c);
        } else if (!Character.isSpaceChar(c)) {
            state = State.ATTRIBUTE_START;
            attributeNameStartIndex = tagBuffer.length();
            tagBuffer.append(c);
        } else {
            tagBuffer.append(c);
        }
    }

    private void matchAttribute(char c) throws IOException {
        if (c == '=' || Character.isSpaceChar(c)) {
            state = State.ATTRIBUTE;

            // match the attribute
            String attributeName = tagBuffer.substring(attributeNameStartIndex);
            currentAttribute = findMatchingAttribute(attributeName);

            attributeNameStartIndex = -1;

            if (c == '=') {
                matchInAttribute(c);
            } else {
                tagBuffer.append(c);
            }
        } else {
            // still in attribute
            tagBuffer.append(c);
        }
    }

    private void matchInAttribute(char c) throws IOException {
        if (c == '=') {
            state = State.ATTRIBUTE_EQUALS;
            tagBuffer.append(c);
        } else if (!Character.isSpaceChar(c)) {
            state = State.ATTRIBUTE_START;
            attributeNameStartIndex = tagBuffer.length();
            matchAttribute(c);
        } else {
            // space
            tagBuffer.append(c);
        }
    }

    private void matchValueStart(char c) {
        if (c == '"') {
            state = State.VALUE_START_DQ;
            tagBuffer.append(c);
            markValueStart();
        } else if (c == '\'') {
            state = State.VALUE_START_SQ;
            tagBuffer.append(c);
            markValueStart();
        } else if (!Character.isSpaceChar(c)) {
            state = State.VALUE_START_UQ;
            markValueStart();
            matchUnquotedValue(c);
        } else {
            // space
            tagBuffer.append(c);
        }
    }

    private void matchUnquotedValue(char c) {
        if (Character.isSpaceChar(c)) {
            state = State.VALUE;
            markValueLength();
            tagBuffer.append(c);
            state = State.TAG;
        } else {
            tagBuffer.append(c);
        }
    }

    private void matchSingleQuotedValue(char c) {
        if (c == '\'') {
            state = State.VALUE;
            markValueLength();
            tagBuffer.append(c);
            state = State.TAG;
        } else {
            tagBuffer.append(c);
        }
    }

    private void matchDoubleQuotedValue(char c) {
        if (c == '"') {
            state = State.VALUE;
            markValueLength();
            tagBuffer.append(c);
            state = State.TAG;
        } else {
            tagBuffer.append(c);
        }
    }

    private void processTagBuffer() throws IOException {
        int index = 0;
        for (HtmlAttributeValue attributeValue : attributes) {
            out.write(tagBuffer.substring(index, attributeValue.start));

            if (attributeValue.attr == LavenderHtmlAttribute.STYLE) {
                rewriteCss(attributeValue);
            } else {
                UrlRewriteMatcher matcher = lookupRewriteMatcher(currentTag, attributeValue.attr, attributes);
                String value;

                value = attributeValue.getValue();
                if (matcher != null && attributeValue.attr == LavenderHtmlAttribute.SRCSET) {
                    rewriteSrcSet(attributeValue.getValue());
                } else if (matcher != null && !matcher.ignoreValue(value)) {
                    matchesRewriteUrl(value);
                } else {
                    out.write(value);
                }
            }

            index = attributeValue.end;
        }

        out.write(tagBuffer.substring(index));

        attributeNameStartIndex = -1;
        attributes.clear();
        tagBuffer.setLength(0);
        uriBuffer.setLength(0);
    }

    private void matchesRewriteUrl(String attributeValue) throws IOException {
        String str = rewriteEngine.rewrite(attributeValue, baseURI, contextPath);
        out.write(str);
    }

    private void rewriteCss(HtmlAttributeValue htmlAttributeValue) throws IOException {
        CssProcessor cssProcessor = new CssProcessor();
        cssProcessor.setRewriteEngine(rewriteEngine, baseURI, contextPath);
        cssProcessor.setWriter(out);
        cssProcessor.process(tagBuffer, htmlAttributeValue.start, htmlAttributeValue.end - htmlAttributeValue.start);
    }

    private void rewriteSrcSet(String attributeValue) throws IOException {
        Matcher elements = SRCSET_PATTERN.matcher(attributeValue);
        StringBuffer replacement = new StringBuffer();
        while (elements.find()) {
            String url = elements.group(2);
            if (!url.startsWith("data:")) {
                url = rewriteEngine.rewrite(url, baseURI, contextPath);
            }
            elements.appendReplacement(replacement, new StringBuilder()
                    .append(elements.group(1))
                    .append(url)
                    .append(elements.group(3))
                    .append(elements.group(4))
                    .append(elements.group(5))
                    .toString());
        }
        elements.appendTail(replacement);
        out.write(replacement.toString());
    }

    private void markValueStart() {
        if (currentAttribute != OTHER_HTML_ATTRIBUTE) {
            attributeValueStartIndex = tagBuffer.length();
        }
    }

    private void markValueLength() {
        int attributeEndInTagBuffer = tagBuffer.length();
        if (currentAttribute != OTHER_HTML_ATTRIBUTE) {
            attributes.add(new HtmlAttributeValue(currentAttribute, attributeValueStartIndex, attributeEndInTagBuffer,
                    tagBuffer));
            attributeValueStartIndex = -1;
        }
    }

    private HtmlTag findTagByName(String name) {
        for (HtmlTag tag : knownTags) {
            if (name.equals(tag.getName())) {
                return tag;
            }
        }

        return OTHER_HTML_TAG;
    }

    private HtmlAttribute findMatchingAttribute(String name) {
        for (HtmlAttribute attribute : knownAttributes) {
            if (attribute.attributeMatches(name)) {
                return attribute;
            }
        }
        return OTHER_HTML_ATTRIBUTE;
    }

    private UrlRewriteMatcher lookupRewriteMatcher(HtmlTag tag, HtmlAttribute attribute, List<HtmlAttributeValue> attributeValues) {
        HtmlElement htmlElement = new HtmlElement(tag, attributeValues);

        for (UrlRewriteMatcher urlRewriteMatcher : urlRewriteMatchers) {
            if (attribute == urlRewriteMatcher.getAttributeToRewrite() && urlRewriteMatcher.matches(htmlElement)) {
                return urlRewriteMatcher;
            }
        }
        return null;
    }
}
