package com.github.cimsbioko.idschemetest.indexing;


import com.github.cimsbioko.idschemetest.shared.CustomAnalyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

import javax.annotation.Resource;

import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE;
import static org.apache.lucene.util.Version.LUCENE_36;

/**
 * Builds an index from an sql query.
 */
public class IndexBuilderImpl implements IndexBuilder {

    private static Logger log = LoggerFactory.getLogger(IndexBuilderImpl.class);

    private File indexFile;

    private String preQuery;
    private String query;
    private String postQuery;

    private JdbcTemplate jdbcTemplate;

    @Resource(name = "indexFile")
    public void setIndexDir(File indexFile) {
        this.indexFile = indexFile;
    }

    @Resource(name = "preQuery")
    public void setPreQuery(String ddl) {
        this.preQuery = ddl;
    }

    @Resource(name = "query")
    public void setQuery(String query) {
        this.query = query;
    }

    @Resource(name = "postQuery")
    public void setPostQuery(String ddl) {
        this.postQuery = ddl;
    }

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Rebuilds the search index. Needs to be run before search is possible.
     */
    @Override
    @Transactional
    public void rebuild() {

        log.info("rebuilding search index");

        long start = System.currentTimeMillis();

        if (preQuery != null) {
            log.info("running pre-query setup");
            jdbcTemplate.execute(preQuery);
        }

        log.info("rebuilding index at {}", indexFile.getAbsolutePath());

        // Create the index directory if it doesn't already exist
        if (indexFile.mkdirs()) {
            log.info("created index directory");
        }

        try {
            Analyzer analyzer = new CustomAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(LUCENE_36, analyzer);
            config.setRAMBufferSizeMB(5.0f);
            config.setOpenMode(CREATE);
            Directory indexDir = FSDirectory.open(indexFile);
            IndexWriter indexWriter = new IndexWriter(indexDir, config);
            RowIndexer rowHandler = new RowIndexer(indexWriter);
            PreparedStatementCreator streamStmtCreator = new MySqlStreamingStatementCreator(query);
            try {
                jdbcTemplate.query(streamStmtCreator, rowHandler);
                indexWriter.commit();
            } finally {
                indexWriter.close();
            }
        } catch (Exception e) {
            log.warn("indexing process aborted early", e);
        }

        if (postQuery != null) {
            log.info("running post-query tear-down");
            jdbcTemplate.execute(postQuery);
        }

        if (log.isInfoEnabled()) {
            log.info(String.format("index rebuild complete (total %.3f sec)", (System.currentTimeMillis() - start) / 1000.0));
        }
    }
}
