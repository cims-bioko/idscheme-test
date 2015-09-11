package com.github.cimsbioko.idschemetest.searching;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;

import static org.apache.lucene.util.Version.LUCENE_36;

/**
 * A customized lucene {@link QueryParser} that overrides some default configuration and interprets
 * age terms as numeric range queries.
 */
public class CustomQueryParser extends QueryParser {

    public CustomQueryParser(Analyzer analyzer) {
        super(LUCENE_36, "dip", analyzer);
        setAllowLeadingWildcard(true); // Just in case we want to do sql %-style queries
        setAutoGeneratePhraseQueries(true); // Lazy way to parse phrases - works for this system
        setDefaultOperator(Operator.OR);  // Because we rely on it, it's default
    }

    @Override
    protected Query getRangeQuery(String field, String part1, String part2, boolean inclusive) throws ParseException {
        if ("age".equals(field)) {
            int start = Integer.parseInt(part1), end = Integer.parseInt(part2);
            return NumericRangeQuery.newIntRange("age", start, end, inclusive, inclusive);
        }
        return super.getRangeQuery(field, part1, part2, inclusive);
    }
}
