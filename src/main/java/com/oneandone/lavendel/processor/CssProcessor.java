package com.oneandone.lavendel.processor;

import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * {@link Processor} for CSS content. This implementation scans the content for <code>url(...)</code> pattern and
 * rewrites found URIs.
 * @author seelmann
 */
public class CssProcessor extends AbstractProcessor {

    /** The logger. */
    static final Logger LOG = Logger.getLogger(CssProcessor.class);

    /** The state of this processor. */
    protected State state = State.OTHER;

    /**
     * Instantiates a new CSS processor.
     */
    public CssProcessor() {
        super(LOG);
    }

    /**
     * An enum to track the state of this processor.
     */
    enum State {
        /** The 'U' of URL */
        URL_U(true, 'u', 'U'),

        /** The 'R' or URL */
        URL_R(true, 'r', 'R'),

        /** The 'L' of URL */
        URL_L(true, 'l', 'L'),

        /** The left parenthesis after URL */
        URL_LPAR(true, '('),

        /** The default state. */
        OTHER(false, ')');

        private boolean strict;
        private char[] chars;

        State(boolean strict, char... chars) {
            this.strict = strict;
            this.chars = chars;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void process(char c) throws IOException {
        // url(
        switch (state) {
        case OTHER:
            match(c, State.URL_U);
            break;
        case URL_U:
            match(c, State.URL_R);
            break;
        case URL_R:
            match(c, State.URL_L);
            break;
        case URL_L:
            match(c, State.URL_LPAR);
            break;
        case URL_LPAR:
            processUrl(c);
            break;

        default:
            throw new IllegalStateException("Unexpected state: " + state);
        }
    }

    protected void processUrl(char c) throws IOException {
        if (c == ')') {
            rewriteUrl();
            state = State.OTHER;
            out.write(c);
        } else {
            uriBuffer.append(c);
        }
    }

    /**
     * Rewrites the URL stored in urlBuffer.
     */
    protected void rewriteUrl() throws IOException {
        out.write(rewriteEngine.rewrite(uriBuffer.toString(), baseURI, contextPath));
        uriBuffer.setLength(0);
    }

    protected void match(char c, State... states) throws IOException {
        State newState = null;
        outer: for (State s : states) {
            for (char k : s.chars) {
                if (c == k) {
                    state = s;
                    newState = s;
                    break outer;
                }
            }
        }

        // reset state
        if (newState == null && state.strict) {
            state = State.OTHER;
        }

        out.write(c);
    }

}
