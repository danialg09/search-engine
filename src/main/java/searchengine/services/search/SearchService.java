package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.searching.RelevanceItem;
import searchengine.dto.searching.SearchingResponse;
import searchengine.dto.searching.SearchingData;
import searchengine.exception.IndexingException;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.services.lemmatization.LemmaService;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaService lemmaService;

    private static final String EMPTY_QUERY = "Задан пустой поисковый запрос";

    @Transactional(readOnly = true)
    public SearchingResponse search(String query, String site, int offset, int limit) {
        if (query.isEmpty()) {
            throw new IndexingException(EMPTY_QUERY);
        }

        List<SearchingData> data = search(query, site);

        Pageable pageable = PageRequest.of(offset / limit, limit);

        org.springframework.data.domain.Page<SearchingData> page = paginateResults(data, pageable);

        SearchingResponse result = new SearchingResponse();
        result.setResult("true");
        result.setCount(page.getTotalElements());
        result.setData(page.getContent());

        return result;
    }

    public List<SearchingData> search(String query, String site) {
        log.debug("Search query: {}", query);
        List<SearchingData> data = new ArrayList<>();
        log.debug("Search site: {}", site);
        List<String> lemmas = List.copyOf(lemmaService.getLemmas(query).keySet());
        log.debug("After getting lemmas: {}", lemmas);
        boolean allSites = site.isEmpty();
        long totalPages = pageRepository.count();
        double threshold = totalPages * 0.7;

        log.debug("Sorting lemmas");
        List<Lemma> sortedLemmas = lemmas.stream()
                .flatMap(lemmaStr -> lemmaRepository.findAllByLemma(lemmaStr).stream())
                .filter(l -> allSites || l.getSite().getUrl().equals(site))
                .filter(l -> l.getFrequency() < threshold)
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .toList();
        log.debug("After sorting lemmas");

        log.debug("Filtering pages");
        List<Page> pages = filterPagesByLemmas(sortedLemmas, site);
        if (pages.isEmpty()) return Collections.emptyList();

        log.debug("Calculating relevance");
        List<RelevanceItem> relevanceItems = calculateRelevance(pages);

        for (RelevanceItem relevanceItem : relevanceItems) {
            SearchingData sd = createData(relevanceItem, lemmas);
            if (sd != null) data.add(sd);
        }

        return data;
    }

    public SearchingData createData(RelevanceItem item, List<String> lemmas) {
        log.info("Creating data for lemmas: {}", lemmas);
        SearchingData data = new SearchingData();
        Page page = item.getPage();
        String html = page.getContent();
        Document doc = Jsoup.parse(html);
        String snippet = getSnippet(page, lemmas);

        data.setSite(page.getSite().getUrl());
        data.setSiteName(page.getSite().getName());
        data.setTitle(doc.title());
        data.setRelevance(item.getRelevance());
        data.setUri(page.getPath());
        data.setSnippet(snippet);

        return snippet.isBlank() ? null : data;
    }

    public String getSnippet(Page page, List<String> lemmas) {
        String text = Jsoup.parse(page.getContent()).body().text();
        String lemma = lemmas.get(0).toLowerCase();
        String[] words = text.split("\\s+");

        int lemmaIndex = -1;
        for (int i = 0; i < words.length; i++) {
            if (words[i].toLowerCase().contains(lemma)) {
                lemmaIndex = i;
                break;
            }
        }
        if (lemmaIndex == -1) {
            return "";
        }

        int start = Math.max(0, lemmaIndex - 10);
        int end = Math.min(words.length, lemmaIndex + 11);

        StringBuilder snippet = new StringBuilder();

        if (start > 0) snippet.append("... ");

        for (int i = start; i < end; i++) {
            String word = words[i];
            boolean isLemma = lemmas.stream()
                    .anyMatch(l -> word.toLowerCase().startsWith(l.toLowerCase()));

            if (isLemma) {
                snippet.append("<b>").append(word).append("</b>");
            } else {
                snippet.append(word);
            }

            if (i < end - 1) snippet.append(" ");
        }

        if (end < words.length) snippet.append(" ...");

        return snippet.toString();
    }

    public List<RelevanceItem> calculateRelevance(List<Page> pages) {
        log.info("Calculating relevance");
        List<RelevanceItem> items = pages.stream().map(page -> {
            double sum = indexRepository.findAllByPageId(page.getId())
                    .stream()
                    .mapToDouble(Index::getRank).sum();

            return new RelevanceItem(page, sum);
        }).sorted(Comparator.comparing(RelevanceItem::getRelevance).reversed()).toList();

        double max = items.get(0).getRelevance();

        return items.stream()
                .map(item -> new RelevanceItem(item.getPage(), item.getRelevance() / max))
                .sorted(Comparator.comparing(RelevanceItem::getRelevance).reversed()).toList();
    }
    
    public List<Page> filterPagesByLemmas(List<Lemma> sortedLemmas, String site) {
        boolean allSites = site.isEmpty();
        if (sortedLemmas.isEmpty()) return Collections.emptyList();

        Map<String, List<Lemma>> grouped = sortedLemmas.stream()
                .collect(Collectors.groupingBy(Lemma::getLemma));

        Set<Integer> commonPageIds = null;

        for (List<Lemma> lemmaGroup : grouped.values()) {
            Set<Integer> pageIdsForLemma = lemmaGroup.stream()
                    .flatMap(l -> indexRepository.findAllByLemmaId(l.getId()).stream())
                    .map(Index::getPage)
                    .filter(p -> allSites || p.getSite().getUrl().equals(site))
                    .map(Page::getId)
                    .collect(Collectors.toSet());

            if (commonPageIds == null) {
                commonPageIds = pageIdsForLemma;
            } else {
                commonPageIds.retainAll(pageIdsForLemma);
            }

            if (commonPageIds.isEmpty()) return Collections.emptyList();
        }

        return pageRepository.findAllById(commonPageIds);
    }

    private org.springframework.data.domain.Page<SearchingData> paginateResults(List<SearchingData> allResults, Pageable pageable) {
        int start = pageable.getPageNumber() * pageable.getPageSize();
        if (start >= allResults.size()) return org.springframework.data.domain.Page.empty(pageable);

        int end = Math.min(start + pageable.getPageSize(), allResults.size());

        List<SearchingData> paginatedResults = allResults.subList(start, end);

        return new PageImpl<>(paginatedResults, pageable, allResults.size());
    }
}
