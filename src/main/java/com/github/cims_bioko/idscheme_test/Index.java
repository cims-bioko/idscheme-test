package com.github.cims_bioko.idscheme_test;

import com.github.cims_bioko.idscheme_test.exceptions.BadQueryException;
import com.github.cims_bioko.idscheme_test.exceptions.NoIndexException;

import java.io.IOException;
import java.util.Map;

public interface Index {

    void rebuild();

    String buildQuery(Map<String, Object> params);

    SearchResult search(String query, int maxResults) throws NoIndexException, BadQueryException, IOException;

}
