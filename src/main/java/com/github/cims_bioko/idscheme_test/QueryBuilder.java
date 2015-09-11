package com.github.cims_bioko.idscheme_test;

import java.util.Map;

/**
 * Created by batkinson on 9/10/15.
 */
interface QueryBuilder {
    String buildQuery(Map<String, Object> params);
}
