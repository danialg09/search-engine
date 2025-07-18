package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SearchEngineProperties;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.exception.IndexingException;
import searchengine.indexing.WebCrawlerTask;
import searchengine.model.*;
import searchengine.repository.SiteRepository;
import searchengine.services.lemmatization.LemmaService;
import searchengine.services.siteops.SiteDataService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private static final String OUTSIDE_CONFIG_FILE =
            "Данная страница находится за пределами сайтов, указанных в конфигурационном файле";
    private static final String ALREADY_STARTED = "Индексация уже запущена";
    private static final String NOT_STARTED = "Индексация не запущена";

    private final SearchEngineProperties properties;
    private final SiteDataService siteDataService;
    private final SiteRepository siteRepository;
    private final LemmaService lemmaService;

    private ForkJoinPool pool = new ForkJoinPool();

    private final SitesList sites;

    @Transactional
    public void startIndexing() {
        if (RUNNING.get()) {
            throw new IndexingException(ALREADY_STARTED);
        }
        ExecutorService executorService = Executors.newFixedThreadPool(sites.getSites().size());

        for (SiteConfig siteConfig : sites.getSites()) {
            siteDataService.deleteAllBySite(siteConfig);
            Site entity = siteDataService.createSite(siteConfig);
            siteDataService.updateStatus(entity, Status.INDEXING);
            executorService.submit(
                    () -> indexing(entity, entity.getUrl(), false)
            );
        }
        executorService.shutdown();
    }

    private void indexing(Site entity, String url, boolean isSinglePage) {
        RUNNING.set(true);
        log.info("Running set true for - {}", entity.getName());
        log.info("Indexing started for: {}", entity.getName());


        if (pool.isShutdown() || pool.isTerminated()) {
            pool = new ForkJoinPool();
            log.info("ForkJoinPool restarted");
        }

        ConcurrentHashMap<String, String> siteMap = new ConcurrentHashMap<>();
        WebCrawlerTask task = new WebCrawlerTask(
                siteMap, properties, lemmaService, siteDataService,
                entity, 0, entity.getUrl(), url, isSinglePage
        );

        log.info("FJP Invoked tree {} with root {}", task, entity.getName());
        pool.invoke(task);
        pool.shutdown();

        try {
            pool.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Indexing interrupted for {}", entity.getName(), e);
            return;
        } finally {
            RUNNING.set(false);
            log.info("RUNNING SET FALSE");
        }
        log.info("Indexing finished for: {}", entity.getName());

        Site updated = siteRepository.findByUrl(entity.getUrl()).orElse(entity);
        Status finalStatus = updated.getStatus().equals(Status.FAILED) ? Status.FAILED : Status.INDEXED;
        siteDataService.updateStatus(updated, finalStatus);
    }

    @Transactional
    public void stopFullIndexing() {
        if (!RUNNING.get()) {
            throw new IndexingException(NOT_STARTED);
        }
        pool.shutdownNow();

        for (Site site : siteRepository.findAll()) {
            if (site.getStatus() != Status.INDEXED) {
                site.setStatus(Status.FAILED);
                site.setLastError("Индексация остановлена пользователем");
            }
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }
        RUNNING.set(false);
    }

    @Transactional
    public void indexPage(String url) {

        Optional<SiteConfig> found = sites.getSites().stream()
                .filter(siteConfig -> url.startsWith(siteConfig.getUrl()))
                .findFirst();
        if (found.isEmpty()) {
            throw new IndexingException(OUTSIDE_CONFIG_FILE);
        }

        Site entity = siteRepository.findByUrl(found.get().getUrl()).orElseThrow();
        String path = url.substring(found.get().getUrl().length());
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        siteDataService.deleteDataByPage(path, entity);
        siteDataService.updateStatus(entity, Status.INDEXING);
        indexing(entity, path, true);
    }
}
