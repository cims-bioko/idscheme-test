package com.github.cims_bioko.idscheme_test;

import com.samskivert.mustache.Mustache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mustache.MustacheEnvironmentCollector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;

import java.io.IOException;
import java.nio.file.Files;

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
