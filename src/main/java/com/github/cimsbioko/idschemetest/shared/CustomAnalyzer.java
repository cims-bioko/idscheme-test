package com.github.cimsbioko.idschemetest.shared;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

import java.io.Reader;

import static org.apache.lucene.util.Version.LUCENE_36;

/**
 * A custom lucene {@link Analyzer} that processes input during indexing. It processes input into a
 * stream of lowercase alphanumeric tokens.
 */
public class CustomAnalyzer extends ReusableAnalyzerBase {
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer src = new AlphaNumericTokenizer(LUCENE_36, reader);
        TokenStream tok = new LowerCaseFilter(LUCENE_36, src);
        return new TokenStreamComponents(src, tok);
    }
}
