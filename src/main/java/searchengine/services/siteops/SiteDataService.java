package searchengine.services.siteops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConfig;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteDataService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Transactional
    public Site createSite(SiteConfig siteConfig) {
        Site site = new Site();
        site.setUrl(siteConfig.getUrl());
        site.setName(siteConfig.getName());
        site.setStatus(Status.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        log.debug("Site creation finished");
        return siteRepository.save(site);
    }

    public List<Page> createPagesBatch(List<Page> pages) {
        log.info("Saved {} pages", pages.size());
        return pageRepository.saveAll(pages);
    }

    public Set<String> checkExistingPages(List<String> pages) {
        return new HashSet<>(pageRepository.findPathsByPathIn(pages));
    }

    @Transactional
    public void deleteAllBySite(SiteConfig siteConfig) {
        Site exists = siteRepository.findByUrl(siteConfig.getUrl()).orElse(null);
        if (exists != null) {
            List<Page> pages = pageRepository.findAllBySiteId(exists.getId());
            for (Page page : pages) {
                indexRepository.deleteAllByPage(page);
            }
            lemmaRepository.deleteAllBySiteId(exists.getId());
            pageRepository.deleteAllBySiteId(exists.getId());
            siteRepository.delete(exists);
        }
        log.debug("Data for SiteConfig deleted");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLemma(Site site, Page page, String lemma, Integer count) {
        log.debug("Method saveLemma of SiteDataService with lemma {}", lemma);
        Lemma exist = lemmaRepository.findByLemmaAndSite(lemma, site).orElse(null);

        if (exist != null) {
            log.debug("Found lemma not null {}", exist.getLemma());
            lemmaRepository.incrementFrequencyById(exist.getId());
            checkForSave(exist, page, count);
        } else {
            exist = Lemma.builder()
                    .lemma(lemma).site(site).frequency(1).build();
            lemmaRepository.save(exist);
            log.debug("Saved lemma {}", exist.getLemma());

            checkForSave(exist, page, count);
        }
        log.debug("End of method saveLemma of SiteDataService with lemma {}", lemma);
    }

    @Transactional
    public void checkForSave(Lemma lemma, Page page, Integer count) {
        Optional<Index> exists = indexRepository.findByLemmaIdAndPageId(lemma.getId(), page.getId());
        if (exists.isEmpty()) {
            Index index = Index.builder()
                    .lemma(lemma)
                    .page(page)
                    .rank(count.floatValue())
                    .build();
            indexRepository.save(index);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(Site site, Status status) {
        Site exists = siteRepository.findById(site.getId()).orElse(null);
        if (exists != null) {
            exists.setStatus(status);
            exists.setStatusTime(LocalDateTime.now());
            siteRepository.save(exists);
            log.info("Site {} status change done {}", site, status);
        }
    }

    @Transactional
    public void updateStatusTime(Site site) {
        Site exists = siteRepository.findById(site.getId()).orElse(null);
        if (exists != null) {
            exists.setStatusTime(LocalDateTime.now());
            siteRepository.save(exists);
        }
    }

    @Transactional
    public void updateLastError(Site site, String error) {
        site.setStatus(Status.FAILED);
        site.setLastError(error);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    @Transactional(readOnly = true)
    public boolean pageExists(String path) {
        return pageRepository.findByPath(path).isPresent();
    }

    @Transactional
    public void deleteDataByPage(String path, Site site) {
        Optional<Page> page = pageRepository.findByPathAndSite(path, site);
        if (page.isPresent()) {
            List<Index> index = indexRepository.findAllByPageId(page.get().getId());
            index.forEach(i -> {
                lemmaRepository.decrementFrequencyById(i.getLemma().getId());
            });

            indexRepository.deleteAllByPage(page.get());
            pageRepository.deleteById(page.get().getId());
            lemmaRepository.deleteAllByFrequencyZero();
        }
        log.info("Data for Page {} deleted", path);
    }
}
