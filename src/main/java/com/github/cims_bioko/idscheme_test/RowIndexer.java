package com.github.cims_bioko.idscheme_test;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A callback for {@link JdbcTemplate} that indexes our query results as a lucene document using
 * the supplied {@link IndexWriter}.
 */
class RowIndexer implements RowCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(RowIndexer.class);

    private static final Set<String> NAME_LABELS = new HashSet<>(Arrays.asList("firstName", "middleName", "lastName"));
    private static final Set<String> PHONE_LABELS = new HashSet<>(Arrays.asList("phoneNumber", "otherPhoneNumber", "pointOfContactPhoneNumber"));
    private static final Set<String> HEAD_NAME_LABELS = new HashSet<>(Arrays.asList("hhFirstName", "hhMiddleName", "hhLastName"));

    private int processed = 0;
    private IndexWriter indexWriter;

    public RowIndexer(IndexWriter indexWriter) {
        this.indexWriter = indexWriter;
    }

    @Override
    public void processRow(ResultSet rs) throws SQLException {

        if (log.isDebugEnabled()) {
            log.debug(Integer.toString(++processed) + ": " + rs.getString("extId"));
        }

        // Used to get field count and labels
        ResultSetMetaData md = rs.getMetaData();

            /*
               Used to merge multiple fields into single term without duplication.
               This prevents skewing the scores when value is repeated multiple times.
             */
        Set<String> names = new HashSet<>();
        Set<String> phones = new HashSet<>();
        Set<String> headNames = new HashSet<>();

        boolean droppedFuzzy = false;

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
                } else if (NAME_LABELS.contains(label)) {
                    String rawNames = rs.getString(label);
                    // Field is only used to show result, not used in search
                    f = new Field(label, rawNames, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO);
                    for (String name : rawNames.split("\\s+")) {
                        name = name.replaceAll("(?U)\\W+", "").toLowerCase();
                        if (names.contains(name)) {
                            continue;
                        }
                        if (containsFuzzyMatch(names, name)) {
                            log.debug("dropping fuzzy duplicate: {}", name);
                            droppedFuzzy = true;
                            continue;
                        }
                        names.add(name);
                    }
                } else if (HEAD_NAME_LABELS.contains(label)) {
                    String rawNames = rs.getString(label);
                    // Field is only used to show result, not used in search
                    f = new Field(label, rawNames, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO);
                    for (String name : rawNames.split("\\s+")) {
                        name = name.replaceAll("(?U)\\W+", "").toLowerCase();
                        if (headNames.contains(name)) {
                            continue;
                        }
                        if (containsFuzzyMatch(headNames, name)) {
                            log.debug("dropping fuzzy duplicate: {}", name);
                            droppedFuzzy = true;
                            continue;
                        }
                        headNames.add(name);
                    }
                } else {
                    f = new Field(label, rs.getString(label), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES);
                }
                d.add(f);
            }
        }

        if (droppedFuzzy) {
            log.debug("indexing names: {}", names);
            log.debug("indexing head names: {}", headNames);
        }

        // Construct single de-duplicated name term from name terms
        if (!names.isEmpty()) {
            StringBuilder nameBuf = new StringBuilder();
            for (String name : names) {
                if (nameBuf.length() > 0)
                    nameBuf.append(" ");
                nameBuf.append(name);
            }
            d.add(new Field("name", nameBuf.toString(), Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.YES));
        }

        // Construct single de-duplicated phone term from phone terms
        if (!phones.isEmpty()) {
            StringBuilder phoneBuf = new StringBuilder();
            for (String phone : phones) {
                if (phoneBuf.length() > 0)
                    phoneBuf.append(" ");
                phoneBuf.append(phone);
            }
            d.add(new Field("phone", phoneBuf.toString(), Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.YES));
        }

        if (!headNames.isEmpty()) {
            StringBuilder nameBuf = new StringBuilder();
            for (String name : headNames) {
                if (nameBuf.length() > 0)
                    nameBuf.append(" ");
                nameBuf.append(name);
            }
            d.add(new Field("headName", nameBuf.toString(), Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.YES));
        }

        try {
            indexWriter.addDocument(d);
        } catch (IOException e) {
            log.warn("failed to add document to index", e);
        }
    }

    private static final float MAX_SIMILARITY = 0.99f;

    private boolean containsFuzzyMatch(Set<String> values, String value) {
        JaroWinklerDistance jwd = new JaroWinklerDistance();
        for (String v : values) {
            if (jwd.getDistance(v, value) > MAX_SIMILARITY)
                return true;
        }
        return false;
    }
}
