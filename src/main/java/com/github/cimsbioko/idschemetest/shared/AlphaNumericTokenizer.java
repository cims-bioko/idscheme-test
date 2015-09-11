package com.github.cimsbioko.idschemetest.shared;

import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.Version;

import java.io.Reader;

/**
 * A custom lucene {@link Tokenizer} that is used to generate tokens from an input stream. It defines
 * tokens as being only Alphanumeric strings. All other input is considered as part of a delimiter.
 */
public class AlphaNumericTokenizer extends CharTokenizer {
    public AlphaNumericTokenizer(Version matchVersion, Reader in) {
        super(matchVersion, in);
    }

    @Override
    protected boolean isTokenChar(int c) {
        return Character.isLetterOrDigit(c);
    }
}
