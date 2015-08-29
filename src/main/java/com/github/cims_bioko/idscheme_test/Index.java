package com.github.cims_bioko.idscheme_test;

import java.util.List;
import java.util.Map;

public interface Index {

    void rebuild();

    List<Map<String, Object>> search(Map<String, Object> params, int maxResults) throws NoIndexException;

}
