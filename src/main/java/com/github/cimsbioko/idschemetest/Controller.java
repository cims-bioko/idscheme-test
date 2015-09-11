package com.github.cimsbioko.idschemetest;

import com.github.cimsbioko.idschemetest.exceptions.BadQueryException;
import com.github.cimsbioko.idschemetest.exceptions.NoIndexException;
import com.github.cimsbioko.idschemetest.indexing.IndexBuilder;
import com.github.cimsbioko.idschemetest.searching.IndexSearcher;
import com.github.cimsbioko.idschemetest.searching.QueryBuilder;
import com.github.cimsbioko.idschemetest.searching.SearchResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * The main entry point and web controller for our spring boot application.
 */
@org.springframework.stereotype.Controller
@EnableAutoConfiguration
@ComponentScan(basePackages = {"com.github.cimsbioko.idschemetest"})
@Secured("USER")
public class Controller {

    @Autowired
    private IndexBuilder indexBuilder;

    @Autowired
    private IndexSearcher indexSearcher;

    @Autowired
    private QueryBuilder queryBuilder;

    @RequestMapping("/")
    String search(@RequestParam Map<String, Object> params, Map<String, Object> model) throws IOException {

        // Transfer the params untouched to model
        model.putAll(params);

        // Prune empty values
        Iterator<Map.Entry<String, Object>> entries = params.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Object> entry = entries.next();
            if (entry.getValue() == null || entry.getValue().toString().trim().equals(""))
                entries.remove();
        }

        // Put results in the model
        try {
            boolean manualMode = "on".equals(params.get("manualOverride"));
            String query = manualMode ? params.get("query").toString() : queryBuilder.buildQuery(params);
            model.put("query", query);
            SearchResult result = indexSearcher.search(query, 15);
            model.put("results", result.getResults());
            String summaryMessage = String.format("Showing %s most relevant of %s total hits",
                    result.getResults().size(), result.getTotalHits());
            model.put("resultsSummary", summaryMessage);
        } catch (NoIndexException e) {
            model.put("message", "Index does not exist.");
            model.put("showBuildLink", true);
        } catch (BadQueryException e) {
            model.put("message", "Bad query, please check search criteria.");
        }

        return "index";
    }

    @RequestMapping("/buildIndex")
    @ResponseBody
    String buildIndex() {
        indexBuilder.rebuild();
        return "Built index.";
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Controller.class, args);
    }

}