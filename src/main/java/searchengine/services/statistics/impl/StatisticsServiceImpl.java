package searchengine.services.statistics.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.*;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.siteops.SiteDataService;
import searchengine.services.statistics.StatisticsService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;

    private final SitesList sites;
    private final SiteDataService siteDataService;

    @Override
    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics() {
        log.info("Call of method getStatistics");
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            DetailedStatisticsItem item = statistics(site);
            detailed.add(item);
        }

        total.setPages((int) pageRepository.count());
        total.setLemmas((int) lemmaRepository.count());

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();

        data.setTotal(total);
        data.setDetailed(detailed);

        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    public DetailedStatisticsItem statistics(Site site) {
        SiteEntity exists = siteRepository.findByUrl(site.getUrl()).orElse(null);
        DetailedStatisticsItem item = new DetailedStatisticsItem();

        if (exists != null) {
            Long pages = pageRepository.countPageBySiteId(exists.getId());
            Long lemmas = lemmaRepository.countLemmaBySiteId(exists.getId());
            long timestampMillis = exists.getStatusTime()
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            item.setName(exists.getName());
            item.setUrl(exists.getUrl());
            item.setPages(pages.intValue());
            item.setLemmas(lemmas.intValue());
            item.setStatus(exists.getStatus().name());
            item.setError(exists.getLastError());
            item.setStatusTime(timestampMillis);
        } else {
            SiteEntity entity = siteDataService.createSite(site);

            long timestampMillis = entity.getStatusTime()
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            item.setName(entity.getName());
            item.setUrl(entity.getUrl());
            item.setStatusTime(timestampMillis);
        }
        return item;
    }
}
