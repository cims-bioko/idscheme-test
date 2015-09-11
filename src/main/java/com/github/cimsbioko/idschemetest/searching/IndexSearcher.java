package com.github.cimsbioko.idschemetest.searching;

import com.github.cimsbioko.idschemetest.exceptions.BadQueryException;
import com.github.cimsbioko.idschemetest.exceptions.NoIndexException;

import java.io.IOException;

/**
 * Specification of an index search.
 */
public interface IndexSearcher {
    SearchResult search(String query, int maxResults) throws NoIndexException, BadQueryException, IOException;
}
