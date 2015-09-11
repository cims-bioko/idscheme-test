package com.github.cims_bioko.idscheme_test;

import java.util.Map;

/**
 * Builds a lucene query language query string from the specified parameters.
 */
interface QueryBuilder {
    String buildQuery(Map<String, Object> params);
}
