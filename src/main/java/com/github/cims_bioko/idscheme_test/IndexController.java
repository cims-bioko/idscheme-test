package com.github.cims_bioko.idscheme_test;


import com.samskivert.mustache.Mustache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheEnvironmentCollector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Iterator;
import java.util.Map;

@Controller
@EnableAutoConfiguration
@ComponentScan(basePackages = { "com.github.cims_bioko.idscheme_test"})
public class IndexController {

    @Autowired
    private Index index;

    @Autowired
    private Environment environment;

    private Mustache.Collector collector() {
        MustacheEnvironmentCollector collector = new MustacheEnvironmentCollector();
        collector.setEnvironment(environment);
        return collector;
    }

    @Bean
    public Mustache.Compiler mustacheCompiler(Mustache.TemplateLoader mustacheTemplateLoader) {
        // Required to gracefully handle non-existent properties using defaultValue
        return Mustache.compiler().withLoader(mustacheTemplateLoader).defaultValue("").withCollector(collector());
    }

    @RequestMapping("/")
    String search(@RequestParam Map<String, Object> params, Map<String, Object> model) {

        // Transfer the params untouched to model
        model.putAll(params);

        // Prune empty values
        Iterator<Map.Entry<String,Object>> entries = params.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String,Object> entry = entries.next();
            if (entry.getValue() == null || entry.getValue().toString().trim().equals(""))
                entries.remove();
        }

        // Put results in the model
        try {
            model.put("results", index.search(params, 15));
        } catch (NoIndexException e) {
            model.put("message", "Index does not exist.");
        }

        return "index";
    }

    @RequestMapping("/buildIndex")
    @ResponseBody
    String buildIndex() {
        index.rebuild();
        return "Built index.";
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(IndexController.class, args);
    }

}