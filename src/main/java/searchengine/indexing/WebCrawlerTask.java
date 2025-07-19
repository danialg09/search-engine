package searchengine.indexing;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.SearchEngineProperties;
import searchengine.dto.indexing.PageData;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.lemmatization.LemmaService;
import searchengine.services.siteops.SiteDataService;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

@AllArgsConstructor
@Slf4j
@Builder
public class WebCrawlerTask extends RecursiveTask<Void> {

    private static final Pattern FILE_PATTERN =
            Pattern.compile(".*\\.(pdf|jpg|jpeg|png|gif|bmp|doc|docx|xls|xlsx|ppt|pptx|webp)$"
                    ,Pattern.CASE_INSENSITIVE);

    private final ConcurrentHashMap<String, String> visited;
    private final SearchEngineProperties properties;

    private final LemmaService lemmaService;
    private final SiteDataService service;

    private final Site site;
    private int currentDepth;
    private final String root;
    private String path;
    private boolean onePage;

    @Override
    protected Void compute() {
        log.info("Starting compute - {} by {}", Thread.currentThread().getName(), site.getName());
        if (Thread.currentThread().isInterrupted()) {
            log.warn("Thread is interrupted");
            return null;
        }
        if (currentDepth >= properties.getMaxDepth()) {
            return null;
        }
        if (onePage) {
            saveData(path);
            return null;
        }
        service.updateStatusTime(site);
        log.debug("StatusTime updated successfully");

        List<WebCrawlerTask> subTasks = new ArrayList<>();
        List<String> linkList = getChildLinks(path);
        saveData(path);

        for(String path : linkList) {
            log.debug("Forking subTask for {}", path);
            WebCrawlerTask task = new WebCrawlerTask(visited, properties, lemmaService
                    , service, site, currentDepth + 1, root, path, false);
            task.fork();
            subTasks.add(task);
        }

        for(WebCrawlerTask task : subTasks) {
            if (Thread.currentThread().isInterrupted()) {
                log.warn("Thread is interrupted");
                break;
            }
            log.debug("Joining subTask {}", task.path);
            task.join();
            log.debug("Joined subTask {}", task.path);
        }
        log.info("Finished compute - {} by {}", Thread.currentThread().getName(), site.getName());
        return null;
    }

    private void saveData(String path) {
        log.debug("Saving data from {} by - {}", path, Thread.currentThread().getName());
        String abs = makeUrlAbsolute(path);
        String min = checkLink(abs);

        try {
            PageData pageData = checkContent(abs);
            if (pageData == null || pageData.connection() == null) {
                return;
            }
            Document doc = pageData.connection().get();
            int statusCode = pageData.statusCode();

            Page page = Page.builder()
                    .site(site)
                    .code(statusCode)
                    .content(doc.html())
                    .path(min)
                    .build();
            log.debug("Saving page {}", page.getPath());
            Page saved = service.createPage(page);

            if (saved != null) {
                lemmaService.saveLemmas(site, saved, doc.html());
            }
        } catch (IOException e) {
            log.warn("IOException : {}", e.getMessage());
            service.updateLastError(site, e.getMessage());
        }
    }

    private List<String> getChildLinks(String url) {
        log.debug("Getting child links for {}", url);
        List<String> links = new ArrayList<>();
        String abs = makeUrlAbsolute(url);

        try {
            PageData pageData = checkContent(abs);
            if (pageData == null || pageData.connection() == null) {
                return links;
            }
            Document doc = pageData.connection().get();

            Elements elements = doc.select("a[href]");

            for (Element el : elements) {
                String absLink = el.attr("abs:href");

                if (!absLink.startsWith(root) ||
                        FILE_PATTERN.matcher(absLink).matches() ||
                        absLink.contains("#")) {
                    log.debug("Skipping link {}", absLink);
                    continue;
                }
                String link = checkLink(absLink);

                boolean alreadyInDb = service.pageExists(link);
                log.debug("Already in DB {} for link {}", alreadyInDb, link);

                if (!alreadyInDb && visited.putIfAbsent(absLink, link) == null) {
                    log.debug("Adding link {}", link);
                    links.add(link);
                }
            }

            sleep(properties.getWaitingTime().toMillis());
        } catch (IOException e) {
            log.warn("Exception in method getChildLinks - {}", e.getMessage());
            service.updateLastError(site, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            service.updateLastError(site, "Индексация была прервана");
        }
        return links;
    }

    private String makeUrlAbsolute(String url) {
        if (url.startsWith("https://")) {
            return url;
        }
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        return root + url;
    }

    private String checkLink(String link) {
        String shortLink = link.substring(root.length());

        if (shortLink.isBlank()) shortLink = "/";
        if (!shortLink.startsWith("/")) shortLink = "/" + shortLink;

        return shortLink;
    }

    private PageData checkContent(String abs) throws IOException {
        Connection connection = Jsoup.connect(abs)
                .userAgent(properties.getUserAgent())
                .referrer(properties.getReferrer())
                .timeout(properties.getTimeout());

        Connection.Response response = connection.ignoreContentType(true).execute();

        String contentType = response.contentType();
        if (contentType == null || !contentType.startsWith("text/html")) {
            log.warn("Skipping non-HTML content type: {} from {}", contentType, abs);
            return null;
        }
        return new PageData(connection, response.statusCode());
    }
}
