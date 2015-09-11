package com.github.cimsbioko.idschemetest;

import com.github.cimsbioko.idschemetest.indexing.IndexBuilder;
import com.github.cimsbioko.idschemetest.indexing.IndexBuilderImpl;
import com.github.cimsbioko.idschemetest.searching.IndexSearcher;
import com.github.cimsbioko.idschemetest.searching.IndexSearcherImpl;
import com.github.cimsbioko.idschemetest.searching.IndividualQueryBuilder;
import com.github.cimsbioko.idschemetest.searching.QueryBuilder;
import com.samskivert.mustache.Mustache;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mustache.MustacheEnvironmentCollector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

/**
 * Spring wiring for application.
 */
@Configuration
public class Config {

    @Autowired
    private Environment environment;

    @Value("classpath:sql/pre.sql")
    private Resource preQuery;

    @Value("classpath:sql/query.sql")
    private Resource query;

    @Value("classpath:sql/post.sql")
    private Resource postQuery;

    @Value("#{ systemProperties['java.io.tmpdir'] + '/idscheme-test' }")
    private File indexFile;

    @Bean
    public File indexFile() {
        return indexFile;
    }

    @Bean
    public Mustache.Compiler mustacheCompiler(Mustache.TemplateLoader mustacheTemplateLoader) {
        // Required to gracefully handle non-existent properties using defaultValue
        return Mustache.compiler().withLoader(mustacheTemplateLoader).defaultValue("").withCollector(collector());
    }

    private Mustache.Collector collector() {
        MustacheEnvironmentCollector collector = new MustacheEnvironmentCollector();
        collector.setEnvironment(environment);
        return collector;
    }

    @Bean
    public String preQuery() throws IOException {
        return IOUtils.toString(preQuery.getInputStream());
    }

    @Bean
    public String query() throws IOException {
        return IOUtils.toString(query.getInputStream());
    }

    @Bean
    public String postQuery() throws IOException {
        return IOUtils.toString(postQuery.getInputStream());
    }

    @Bean
    public QueryBuilder queryBuilder() {
        return new IndividualQueryBuilder();
    }

    @Bean
    public IndexBuilder indexBuilder() {
        return new IndexBuilderImpl();
    }

    @Bean
    public IndexSearcher indexSearcher() {
        return new IndexSearcherImpl();
    }
}
