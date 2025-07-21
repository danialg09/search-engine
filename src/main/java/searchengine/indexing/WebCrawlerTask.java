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
import java.util.stream.Collectors;

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
            saveData(Collections.singletonList(path));
            return null;
        }
        if (currentDepth == 0) {
            saveData(Collections.singletonList(path));
        }
        service.updateStatusTime(site);
        log.debug("StatusTime updated successfully");

        List<WebCrawlerTask> subTasks = new ArrayList<>();
        List<String> linkList = getChildLinks(path);
        saveData(linkList);

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

    private void saveData(List<String> linkList) {
        List<Page> pages = new ArrayList<>();
        for (String path : linkList) {
            log.debug("Saving data from {} by - {}", path, Thread.currentThread().getName());
            String abs = checkAbsoluteLink(path);
            String min = checkShortLink(abs);

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
                log.info("Saving page {}", page.getPath());
                pages.add(page);
            } catch (IOException e) {
                log.warn("IOException : {}", e.getMessage());
                service.updateLastError(site, e.getMessage());
            }
        }
        List<Page> saved = service.createPagesBatch(pages);
        saved.stream().filter(Objects::nonNull)
                .forEach(p -> lemmaService.saveLemmas(site, p, p.getContent()));
    }

    private List<String> getChildLinks(String url) {
        log.debug("Getting child links for {}", url);
        List<String> links = new ArrayList<>();
        String abs = checkAbsoluteLink(url);

        try {
            PageData pageData = checkContent(abs);
            if (pageData == null || pageData.connection() == null) return links;

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
                links.add(checkShortLink(absLink));
            }

            sleep(properties.getWaitingTime().toMillis());
        } catch (IOException e) {
            log.warn("Exception in method getChildLinks - {}", e.getMessage());
            service.updateLastError(site, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            service.updateLastError(site, "Индексация была прервана");
        }

        Set<String> existingPaths = service.checkExistingPages(links);

        return links.stream()
                .filter(link -> !existingPaths.contains(link))
                .filter(link -> visited.putIfAbsent(link, link) == null)
                .collect(Collectors.toList());
    }

    private String checkAbsoluteLink(String url) {
        if (url.startsWith("https://")) {
            return url;
        }
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        return root + url;
    }

    private String checkShortLink(String link) {
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
