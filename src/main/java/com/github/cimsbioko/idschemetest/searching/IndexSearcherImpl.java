package com.github.cimsbioko.idschemetest.searching;

import com.github.cimsbioko.idschemetest.shared.CustomAnalyzer;
import com.github.cimsbioko.idschemetest.exceptions.BadQueryException;
import com.github.cimsbioko.idschemetest.exceptions.NoIndexException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NoSuchDirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

/**
 * Performs a search given a lucene search string against a pre-built index.
 */
public class IndexSearcherImpl implements IndexSearcher {

    private static Logger log = LoggerFactory.getLogger(IndexSearcherImpl.class);

    private File indexFile;

    @Resource(name = "indexFile")
    public void setIndexDir(File indexFile) {
        this.indexFile = indexFile;
    }

    @Override
    public SearchResult search(String q, int maxResults) throws NoIndexException, BadQueryException, IOException {

        if ("".equals(q)) {
            throw new BadQueryException();
        }

        try {
            Analyzer analyzer = new CustomAnalyzer();

            // Open the index for the search
            Directory indexDir = FSDirectory.open(indexFile);
            IndexReader reader = IndexReader.open(indexDir);
            org.apache.lucene.search.IndexSearcher searcher = new org.apache.lucene.search.IndexSearcher(reader);
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
