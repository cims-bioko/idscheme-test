package com.github.cimsbioko.idschemetest.searching;

import java.util.List;
import java.util.Map;

/**
 * Result from an index search. Provides information about total number of hits and a limited number
 * of results.
 */
public class SearchResult {

    int totalHits;
    List<Map<String, Object>> results;

    public SearchResult(int totalHits, List<Map<String, Object>> results) {
        this.totalHits = totalHits;
        this.results = results;
    }

    public int getTotalHits() {
        return totalHits;
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }
}
