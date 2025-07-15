package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.response.IndexingResponse;
import searchengine.dto.searching.SearchingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.search.SearchService;
import searchengine.services.indexing.IndexingService;
import searchengine.services.statistics.impl.StatisticsServiceImpl;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final IndexingService indexingService;
    private final StatisticsServiceImpl statisticsServiceImpl;
    private final SearchService searchService;

    @GetMapping("/startIndexing")
    public IndexingResponse startIndexing() {
        indexingService.startIndexing();
        return new IndexingResponse(true);
    }

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsServiceImpl.getStatistics();
    }

    @GetMapping("/stopIndexing")
    public IndexingResponse stopIndexing() {
        indexingService.stopFullIndexing();
        return new IndexingResponse(true);
    }

    @PostMapping("/indexPage")
    public IndexingResponse indexPage(@RequestParam String url) {
        indexingService.indexPage(url);
        return new IndexingResponse(true);
    }

    @GetMapping("/search")
    public SearchingResponse search(@RequestParam String query,
                                    @RequestParam(defaultValue = "") String site,
                                    @RequestParam(defaultValue = "0") int offset,
                                    @RequestParam(defaultValue = "20") int limit) {
        return searchService.search(query, site, offset, limit);
    }
}