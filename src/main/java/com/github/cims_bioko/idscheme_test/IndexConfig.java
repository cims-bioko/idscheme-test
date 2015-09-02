package com.github.cims_bioko.idscheme_test;

import com.samskivert.mustache.Mustache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mustache.MustacheEnvironmentCollector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;

@Configuration
public class IndexConfig {

    @Autowired
    private Environment environment;

    @Value("classpath:pre.sql")
    private Resource preQuery;

    @Value("classpath:query.sql")
    private Resource query;

    @Value("classpath:post.sql")
    private Resource postQuery;

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
        return new String(Files.readAllBytes(preQuery.getFile().toPath()));
    }

    @Bean
    public String query() throws IOException {
        return new String(Files.readAllBytes(query.getFile().toPath()));
    }

    @Bean
    public String postQuery() throws IOException {
        return new String(Files.readAllBytes(postQuery.getFile().toPath()));
    }
}
