package com.github.cims_bioko.idscheme_test;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NoSuchDirectoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE;
import static org.apache.lucene.util.Version.LUCENE_36;

@Component
public class IndexImpl implements Index {

    private File indexFile;
    private JdbcTemplate jdbcTemplate;

    @Value("#{ systemProperties['java.io.tmpdir'] + '/idscheme-test' }")
    public void setIndexDir(File indexFile) {
        this.indexFile = indexFile;
    }

    @Autowired(required = true)
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    static class IndexAnalyzer extends ReusableAnalyzerBase {
        @Override
        protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
            Tokenizer src = new WhitespaceTokenizer(LUCENE_36, reader);
            TokenStream tok = new LowerCaseFilter(LUCENE_36, src);
            return new TokenStreamComponents(src, tok);
        }
    }

    private static final Set<String> NAME_LABELS = new HashSet<>(Arrays.asList("firstName", "middleName", "lastName"));
    private static final Set<String> PHONE_LABELS = new HashSet<>(Arrays.asList("phoneNumber", "otherPhoneNumber", "pointOfContactPhoneNumber"));

    /**
     * Rebuilds the search index. Needs to be run before search is possible.
     */
    @Override
    public void rebuild() {

        PreparedStatementCreator streamStmtCreator = new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                String sql = "select \n" +
                        "\textId,\n" +
                        "\tcase dip when 0 then null else dip end as dip,\n" +
                        "\tcase firstName when 'null' then null else firstName end as firstName,\n" +
                        "\tcase middleName when 'null' then null else middleName end as middleName,\n" +
                        "\tcase lastName when 'null' then null else lastName end as lastName,\n" +
                        "\tdate_format(now(), '%Y') - date_format(dob, '%Y') - (date_format(now(), '00-%m-%d') < date_format(dob, '00-%m-%d')) as age,\n" +
                        "\tcase phoneNumber when 'null' then null else phoneNumber end as phoneNumber,\n" +
                        "\tcase otherPhoneNumber when 'null' then null else otherPhoneNumber end as otherPhoneNumber,\n" +
                        "\tcase pointOfContactPhoneNumber when 'null' then null else pointOfContactPhoneNumber end as pointOfContactPhoneNumber\n" +
                        "from\n" +
                        "\tindividual i" +
                        "\twhere i.deleted != 1";

                // Configures the MySQL java connector to stream data; prevents filling up memory for large results
                PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                stmt.setFetchSize(Integer.MIN_VALUE);
                return stmt;
            }
        };

        System.out.println("rebuilding index at " + indexFile.getAbsolutePath());

        // Create the index directory if it doesn't already exist
        if (indexFile.mkdirs()) {
            System.out.println("created index directory");
        }

        try {
            final Analyzer analyzer = new IndexAnalyzer();

            IndexWriterConfig config = new IndexWriterConfig(LUCENE_36, analyzer);
            config.setRAMBufferSizeMB(5.0f);
            config.setOpenMode(CREATE);

            Directory indexDir = FSDirectory.open(indexFile);

            final IndexWriter indexWriter = new IndexWriter(indexDir, config);

            try {
                long start = System.currentTimeMillis();

                jdbcTemplate.query(streamStmtCreator, new RowCallbackHandler() {

                    private int processed = 0;

                    @Override
                    public void processRow(ResultSet rs) throws SQLException {

                        System.out.println(Integer.toString(++processed) + ": " + rs.getString("extId"));

                        // Used to get field count and labels
                        ResultSetMetaData md = rs.getMetaData();

                        /*
                           Used to merge multiple fields into single term without duplication.
                           This prevents skewing the scores when value is repeated multiple times.
                         */
                        Set<String> names = new HashSet<>();
                        Set<String> phones = new HashSet<>();

                        // Populate document using result set
                        Document d = new Document();
                        for (int col = 1; col <= md.getColumnCount(); col++) {
                            Fieldable f;
                            String label = md.getColumnLabel(col);
                            if (rs.getObject(label) != null) {
                                if ("dip".equals(label)) {
                                    f = new Field(label, rs.getString(label), Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.YES);
                                } else if ("extId".equals(label)) {
                                    // Field is only used to show result, not used in search
                                    f = new Field(label, rs.getString(label), Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO);
                                } else if ("age".equals(label)) {
                                    NumericField nf = new NumericField(label, 1, Field.Store.YES, true);
                                    nf.setIntValue(rs.getInt(label));
                                    f = nf;
                                } else if (PHONE_LABELS.contains(label)) {
                                    // Field is only used to show result, not used in search
                                    f = new Field(label, rs.getString(label), Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO);
                                    for (String phone : rs.getString(label).split("\\s+")) {
                                        phones.add(phone);
                                    }
                                }
                                else if (NAME_LABELS.contains(label)) {
                                    // Field is only used to show result, not used in search
                                    f = new Field(label, rs.getString(label), Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO);
                                    for (String name : rs.getString(label).split("\\s+")) {
                                        names.add(name);
                                    }
                                } else {
                                    f = new Field(label, rs.getString(label), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES);
                                }
                                d.add(f);
                            }
                        }

                        // Construct single de-duplicated name term from name terms
                        if (!names.isEmpty()) {
                            StringBuilder nameBuf = new StringBuilder();
                            for (String name : names) {
                                if (nameBuf.length() > 0)
                                    nameBuf.append(" ");
                                nameBuf.append(name);
                            }
                            d.add( new Field("name", nameBuf.toString(), Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.YES) );
                        }

                        // Construct single de-duplicated phone term from phone terms
                        if (!phones.isEmpty()) {
                            StringBuilder phoneBuf = new StringBuilder();
                            for (String phone : phones) {
                                if (phoneBuf.length() > 0)
                                    phoneBuf.append(" ");
                                phoneBuf.append(phone);
                            }
                            d.add( new Field("phone", phoneBuf.toString(), Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.YES) );
                        }

                        try {
                            indexWriter.addDocument(d);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

                indexWriter.commit();

                System.out.printf("indexing complete (%.3f sec)", (System.currentTimeMillis() - start) / 1000.0);

            } finally {
                indexWriter.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Map<String, Object>> search(Map<String, Object> params, int maxResults) throws NoIndexException {

        List<Map<String, Object>> results = new ArrayList<>();

        try {
            Analyzer analyzer = new IndexAnalyzer();

            // Open the index for the search
            Directory indexDir = FSDirectory.open(indexFile);
            IndexReader reader = IndexReader.open(indexDir);
            IndexSearcher searcher = new IndexSearcher(reader);

            // Build a query from the supplied parameters
            StringBuilder qstr = new StringBuilder();

            if (params.containsKey("dip")) {
                // We boost dip so it dominates score
                qstr.append(String.format("dip:%s^4", params.get("dip")));
            }

            if (params.containsKey("name")) {
                String nameVal = params.get("name").toString();
                for (String name : nameVal.split("\\s+")) {
                    if (qstr.length() > 0)
                        qstr.append(" ");
                    qstr.append(String.format("name:%1$s~", name));
                }
            }

            if (params.containsKey("age")) {
                if (qstr.length() > 0)
                    qstr.append(" ");
                int age = Integer.parseInt(params.get("age").toString());
                qstr.append(String.format("age:[%d TO %d]", age - 3, age + 3));
            }

            if (params.containsKey("phone")) {
                if (qstr.length() > 0)
                    qstr.append(" ");
                String cleansedPhone = params.get("phone").toString().replaceAll("[^0-9]", "");
                qstr.append(String.format("phone:%1$s~", cleansedPhone));
            }

            QueryParser parser = new QueryParser(LUCENE_36, "dip", analyzer) {
                @Override
                protected Query getRangeQuery(String field, String part1, String part2, boolean inclusive) throws ParseException {
                    if ("age".equals(field)) {
                        int start = Integer.parseInt(part1), end = Integer.parseInt(part2);
                        return NumericRangeQuery.newIntRange("age", start, end, inclusive, inclusive);
                    }
                    return super.getRangeQuery(field, part1, part2, inclusive);
                }
            };

            parser.setAllowLeadingWildcard(true); // Just in case we want to do sql %-style queries
            parser.setAutoGeneratePhraseQueries(true); // Lazy way to parse phrases - works for this system
            parser.setDefaultOperator(QueryParser.Operator.OR);  // Because we rely on it, it's default

            Query query = parser.parse(qstr.toString());
            TopDocs hits = searcher.search(query, maxResults);

            for (ScoreDoc sd : hits.scoreDocs) {
                Document d = searcher.doc(sd.doc);
                Map<String, Object> result = new HashMap<>();
                for (Fieldable f : d.getFields()) {
                    result.put(f.name(), f.stringValue());
                }
                result.put("score", sd.score);
                results.add(result);
            }

        } catch (NoSuchDirectoryException nsde) {
            throw new NoIndexException();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
}
