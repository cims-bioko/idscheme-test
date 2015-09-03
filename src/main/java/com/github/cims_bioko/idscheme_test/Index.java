package com.github.cims_bioko.idscheme_test;

import com.github.cims_bioko.idscheme_test.exceptions.BadQueryException;
import com.github.cims_bioko.idscheme_test.exceptions.NoIndexException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Index {

    void rebuild();

    List<Map<String, Object>> search(Map<String, Object> params, int maxResults) throws NoIndexException, BadQueryException, IOException;

}
