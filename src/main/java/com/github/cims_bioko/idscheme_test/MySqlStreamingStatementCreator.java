package com.github.cims_bioko.idscheme_test;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A custom {@link PreparedStatementCreator} for use with {@link JdbcTemplate}. It creates statements
 * configured to stream data efficiently using options for the MySQL java connector. It prevents
 * filling up memory for extremely large results.
 */
class MySqlStreamingStatementCreator implements PreparedStatementCreator {

    private String query;

    public MySqlStreamingStatementCreator(String query) {
        this.query = query;
    }

    @Override
    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
        PreparedStatement stmt = con.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setFetchSize(Integer.MIN_VALUE);
        return stmt;
    }
}
