package com.github.cimsbioko.idschemetest.searching;

import java.util.Map;

/**
 * Builds a lucene query language query string from the specified parameters.
 */
public interface QueryBuilder {
    String buildQuery(Map<String, Object> params);
}
