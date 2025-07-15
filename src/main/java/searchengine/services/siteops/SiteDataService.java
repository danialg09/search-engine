package searchengine.services.siteops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteDataService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Transactional
    public SiteEntity createSite(Site site) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setUrl(site.getUrl());
        siteEntity.setName(site.getName());
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        log.info("SiteEntity creation finished");
        return siteRepository.save(siteEntity);
    }

    @Transactional
    public void deleteAllBySite(Site site) {
        SiteEntity exists = siteRepository.findByUrl(site.getUrl()).orElse(null);
        if (exists != null) {
            List<Page> pages = pageRepository.findAllBySiteId(exists.getId());
            for (Page page : pages) {
                indexRepository.deleteAllByPage(page);
            }
            lemmaRepository.deleteAllBySiteId(exists.getId());
            pageRepository.deleteAllBySiteId(exists.getId());
            siteRepository.delete(exists);
        }
        log.info("Data for Site deleted");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Page createPage(Page page) {
        if (pageRepository.findByPath(page.getPath()).isPresent()) {
            log.info("Page already exists");
            return null;
        }
        log.info("Page creation finished {}", page.getPath());
        return pageRepository.save(page);
    }

    @Transactional
    public void saveLemma(SiteEntity site, Page page, String lemma, Integer count) {
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

    @Transactional
    public void updateStatus(SiteEntity siteEntity, Status status) {
        SiteEntity exists = siteRepository.findById(siteEntity.getId()).orElse(null);
        if (exists != null) {
            exists.setStatus(status);
            exists.setStatusTime(LocalDateTime.now());
            siteRepository.save(exists);
            log.info("SiteEntity {} status change done {}", siteEntity, status);
        }
    }

    @Transactional
    public void updateStatusTime(SiteEntity siteEntity) {
        SiteEntity exists = siteRepository.findById(siteEntity.getId()).orElse(null);
        if (exists != null) {
            exists.setStatusTime(LocalDateTime.now());
            siteRepository.save(exists);
        }
    }

    @Transactional
    public void updateLastError(SiteEntity site, String error) {
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
    public void deleteDataByPage(String path, SiteEntity site) {
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
