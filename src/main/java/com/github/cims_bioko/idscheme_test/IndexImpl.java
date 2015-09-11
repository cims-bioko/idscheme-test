package com.github.cims_bioko.idscheme_test;

import com.github.cims_bioko.idscheme_test.exceptions.BadQueryException;
import com.github.cims_bioko.idscheme_test.exceptions.NoIndexException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NoSuchDirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE;
import static org.apache.lucene.util.Version.LUCENE_36;

/**
 * An abstract search index for use with our web controller. It provides operations to build and
 * search an index based on the supplied queries.
 */
@Component
public class IndexImpl implements Index {

    private static Logger log = LoggerFactory.getLogger(IndexImpl.class);

    private String preQuery;
    private String query;
    private String postQuery;
    private File indexFile;
    private JdbcTemplate jdbcTemplate;

    @Value("#{ systemProperties['java.io.tmpdir'] + '/idscheme-test' }")
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

    @Override
    public SearchResult search(String q, int maxResults) throws NoIndexException, BadQueryException, IOException {

        try {
            Analyzer analyzer = new CustomAnalyzer();

            // Open the index for the search
            Directory indexDir = FSDirectory.open(indexFile);
            IndexReader reader = IndexReader.open(indexDir);
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new CustomQueryParser(analyzer);

            log.info("running query: '{}'", q);

            Query query = parser.parse(q);
            TopDocs hits = searcher.search(query, maxResults);

            log.info("found {} hits, showing top {}", hits.totalHits, maxResults);

            // Convert documents into results map for page
            List<Map<String, Object>> results = new ArrayList<>();
            for (ScoreDoc sd : hits.scoreDocs) {
                Document d = searcher.doc(sd.doc);
                Map<String, Object> result = new HashMap<>();
                for (Fieldable f : d.getFields()) {
                    result.put(f.name(), f.stringValue());
                }
                result.put("score", sd.score);
                results.add(result);
            }

            return new SearchResult(hits.totalHits, results);

        } catch (CorruptIndexException | NoSuchDirectoryException nsde) {
            throw new NoIndexException();
        } catch (ParseException e) {
            throw new BadQueryException();
        }
    }
}

