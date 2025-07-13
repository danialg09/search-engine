package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<IndexingResponse> startIndexing() {
        indexingService.startIndexing();
        return ResponseEntity.ok(new IndexingResponse(true));
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsServiceImpl.getStatistics());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        indexingService.stopFullIndexing();
        return ResponseEntity.ok(new IndexingResponse(true));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam String url) {
        indexingService.indexPage(url);
        return ResponseEntity.ok(new IndexingResponse(true));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchingResponse> search(@RequestParam String query,
                                                    @RequestParam(defaultValue = "") String site,
                                                    @RequestParam(defaultValue = "0") int offset,
                                                    @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }
}
