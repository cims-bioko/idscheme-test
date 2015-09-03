package com.github.cims_bioko.idscheme_test;

import java.util.List;
import java.util.Map;

public class SearchResult {

    int totalHits;
    List<Map<String, Object>> results;

    public SearchResult(int totalHits, List<Map<String, Object>> results) {
        this.totalHits = totalHits;
        this.results = results;
    }

    int getTotalHits() {
        return totalHits;
    }

    List<Map<String, Object>> getResults() {
        return results;
    }
}
