package com.github.cims_bioko.idscheme_test;

import com.github.cims_bioko.idscheme_test.exceptions.BadQueryException;
import com.github.cims_bioko.idscheme_test.exceptions.NoIndexException;

import java.io.IOException;

public interface Index {

    void rebuild();

    SearchResult search(String query, int maxResults) throws NoIndexException, BadQueryException, IOException;

}
