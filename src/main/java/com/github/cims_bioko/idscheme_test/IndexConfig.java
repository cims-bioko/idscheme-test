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

    @Value("classpath:query.sql")
    private Resource storedQuery;

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

    @Bean(name = "query")
    public String query() throws IOException {
        return String.valueOf(Files.readAllBytes(storedQuery.getFile().toPath()));
    }

}
