package com.oneandone.lavendel.processor;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class HtmlProcessor extends AbstractProcessor {

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(HtmlProcessor.class);

    /** The main state of this processor. */
    protected State state = State.NULL;

    /** The current tag. */
    protected Tag tag = Tag.NULL;

    /** The current attribute within an tag. */
    protected Attr attr = Attr.NULL;

    protected int attrIndex = -1;

    /** The tag buffer. */
    protected StringBuilder tagBuffer = new StringBuilder(100);

    /** The relevant attributes in the current tag, in order. */
    protected Map<Attr, Value> attrs = new LinkedHashMap<Attr, Value>();

    /**
     * An enum to track the state of this processor.
     */
    enum State {
        NULL,

        //
        SPECIAL_START, SPECIAL_START_COMMENT_OR_CONDITION, SPECIAL_DOCTYPE, SPECIAL_CDATA, SPECIAL_COMMENT,

        //
        TAG_START, TAG, ATTRIBUTE_START, ATTRIBUTE, ATTRIBUTE_EQUALS, VALUE_START_SQ, VALUE_START_DQ, VALUE_START_UQ, VALUE
    }

    /**
     * An enum to track the current tag.
     */
    enum Tag {
        NULL, IMG, LINK, SCRIPT, INPUT, A, OTHER;

        public static Tag forString(String str) {
            if ("img".equals(str)) {
                return Tag.IMG;
            } else if ("link".equals(str)) {
                return Tag.LINK;
            } else if ("script".equals(str)) {
                return Tag.SCRIPT;
            } else if ("input".equals(str)) {
                return Tag.INPUT;
            } else if ("a".equals(str)) {
                return Tag.A;
            } else {
                return Tag.OTHER;
            }
        }
    }

    /**
     * An enum to track the current attribute.
     */
    enum Attr {
        NULL, SRC, HREF, REL, STYLE, TYPE, OTHER
    }

    private static final class Value {
        private Attr attr;
        private int start;
        private int end;

        private Value(Attr attr, int start) {
            this.attr = attr;
            this.start = start;
        }
    }

    /**
     * Instantiates a new HTML processor.
     */
    protected HtmlProcessor() {
        super(LOG);
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
    public void process(char c) throws IOException {

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

    protected void matchSpecialStart(char c) throws IOException {
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

    protected void matchSpecialStartCommentOrCondition(char c) throws IOException {
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

    protected void matchTagStart(char c) throws IOException {
        if (c == '<') {
            state = State.TAG_START;
        }

        out.write(c);
    }

    protected void matchTag(char c) throws IOException {
        if (Character.isSpaceChar(c)) {
            state = State.TAG;
            tag = Tag.forString(tagBuffer.toString().toLowerCase());
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
            // still in tag
            tagBuffer.append(c);
        }
    }

    protected void matchInTag(char c) throws IOException {
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
            attrIndex = tagBuffer.length();
            tagBuffer.append(c);
        } else {
            tagBuffer.append(c);
        }
    }

    protected void matchAttribute(char c) throws IOException {
        if (c == '=' || Character.isSpaceChar(c)) {
            state = State.ATTRIBUTE;

            // match the attribute
            String a = tagBuffer.substring(attrIndex);
            if ("src".equalsIgnoreCase(a)) {
                attr = Attr.SRC;
            } else if ("href".equalsIgnoreCase(a)) {
                attr = Attr.HREF;
            } else if ("style".equalsIgnoreCase(a)) {
                attr = Attr.STYLE;
            } else if ("rel".equalsIgnoreCase(a)) {
                attr = Attr.REL;
            } else if ("type".equalsIgnoreCase(a)) {
                attr = Attr.TYPE;
            } else {
                attr = Attr.OTHER;
            }

            attrIndex = -1;

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

    protected void matchInAttribute(char c) throws IOException {
        if (c == '=') {
            state = State.ATTRIBUTE_EQUALS;
            tagBuffer.append(c);
        } else if (!Character.isSpaceChar(c)) {
            state = State.ATTRIBUTE_START;
            attrIndex = tagBuffer.length();
            matchAttribute(c);
        } else {
            // space
            tagBuffer.append(c);
        }
    }

    protected void matchValueStart(char c) throws IOException {
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

    protected void matchUnquotedValue(char c) throws IOException {
        if (Character.isSpaceChar(c)) {
            state = State.VALUE;
            markValueLength();
            tagBuffer.append(c);
            state = State.TAG;
        } else {
            tagBuffer.append(c);
        }
    }

    protected void matchSingleQuotedValue(char c) throws IOException {
        if (c == '\'') {
            state = State.VALUE;
            markValueLength();
            tagBuffer.append(c);
            state = State.TAG;
        } else {
            tagBuffer.append(c);
        }
    }

    protected void matchDoubleQuotedValue(char c) throws IOException {
        if (c == '"') {
            state = State.VALUE;
            markValueLength();
            tagBuffer.append(c);
            state = State.TAG;
        } else {
            tagBuffer.append(c);
        }
    }

    protected void processTagBuffer() throws IOException {

        int index = 0;
        for (Attr a : attrs.keySet()) {
            Value value = attrs.get(a);
            out.write(tagBuffer.substring(index, value.start));

            if (tag == Tag.IMG && value.attr == Attr.SRC) {
                rewriteUrl(value);
            } else if (tag == Tag.A && value.attr == Attr.HREF) {
                rewriteUrl(value);
            } else if (tag == Tag.LINK && value.attr == Attr.HREF) {
                boolean rewritten = false;
                if (attrs.containsKey(Attr.REL)) {
                    Value rel = attrs.get(Attr.REL);
                    if ("stylesheet".equalsIgnoreCase(tagBuffer.substring(rel.start, rel.end))) {
                        rewriteUrl(value);
                        rewritten = true;
                    } else if ("icon".equalsIgnoreCase(tagBuffer.substring(rel.start, rel.end))) {
                        rewriteUrl(value);
                        rewritten = true;
                    } else if ("shortcut icon".equalsIgnoreCase(tagBuffer.substring(rel.start, rel.end))) {
                        rewriteUrl(value);
                        rewritten = true;
                    }
                }
                if (!rewritten) {
                    out.write(tagBuffer.substring(value.start, value.end));
                }
            } else if (tag == Tag.SCRIPT && value.attr == Attr.SRC) {
                boolean rewritten = false;
                if (attrs.containsKey(Attr.TYPE)) {
                    Value type = attrs.get(Attr.TYPE);
                    if ("text/javascript".equalsIgnoreCase(tagBuffer.substring(type.start, type.end))) {
                        rewriteUrl(value);
                        rewritten = true;
                    }
                }
                if (!rewritten) {
                    out.write(tagBuffer.substring(value.start, value.end));
                }
            } else if (tag == Tag.INPUT && value.attr == Attr.SRC) {
                boolean rewritten = false;
                if (attrs.containsKey(Attr.TYPE)) {
                    Value type = attrs.get(Attr.TYPE);
                    if ("image".equalsIgnoreCase(tagBuffer.substring(type.start, type.end))) {
                        rewriteUrl(value);
                        rewritten = true;
                    }
                }
                if (!rewritten) {
                    out.write(tagBuffer.substring(value.start, value.end));
                }
            } else if (value.attr == Attr.STYLE) {
                rewriteCss(value);
            } else {
                out.write(tagBuffer.substring(value.start, value.end));
            }

            index = value.end;
        }

        out.write(tagBuffer.substring(index));

        attrIndex = -1;
        attrs.clear();
        tagBuffer.setLength(0);
        uriBuffer.setLength(0);
    }

    protected void rewriteUrl(Value value) throws IOException {
        String str;

        str = rewriteEngine.rewrite(tagBuffer.substring(value.start, value.end), baseURI, contextPath);
        out.write(str);
    }

    protected void rewriteCss(Value value) throws IOException {
        CssProcessor cssProcessor = new CssProcessor();
        cssProcessor.setRewriteEngine(rewriteEngine, baseURI, contextPath);
        cssProcessor.setWriter(out);
        cssProcessor.process(tagBuffer, value.start, value.end - value.start);
    }

    protected void markValueStart() throws IOException {
        if (attrs.containsKey(attr)) {
            attrs.remove(attr);
        }
        if (attr != Attr.OTHER) {
            attrs.put(attr, new Value(attr, tagBuffer.length()));
        }
    }

    protected void markValueLength() throws IOException {
        if (attrs.containsKey(attr)) {
            attrs.get(attr).end = tagBuffer.length();
        }
    }

}
