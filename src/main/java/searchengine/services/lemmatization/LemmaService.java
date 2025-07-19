package searchengine.services.lemmatization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.siteops.SiteDataService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class LemmaService {

    private final SiteDataService service;

    public LuceneMorphology morphology;

    @PostConstruct
    public void init() {
        try {
            morphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Integer> getLemmas(String text) {
        log.debug("Calling method getLemmas - LemmaService");
        Map<String, Integer> lemmas = new HashMap<>();
        text = cleanTags(text).toLowerCase(Locale.ROOT);

        String regex = "\\b[а-яА-ЯёЁ]+\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        try {
            log.debug("Before morphology");
            while (matcher.find()) {

                String word = matcher.group();
                if (filter(word)) continue;

                List<String> wordBaseForms =
                        morphology.getNormalForms(word);
                String base = wordBaseForms.get(0);

                if (lemmas.containsKey(base)) {
                    log.debug("Found lemma {} in database", base);
                    lemmas.put(base, lemmas.get(base) + 1);
                    continue;
                }
                log.debug("Found lemma {} in text", base);
                lemmas.put(base, 1);
            }
            log.debug("After morphology");
        }catch (Exception e){
           log.warn("Error while getting lemmas", e);
        }
        log.debug("End of method getLemmas - LemmaService");
        return lemmas;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLemmas(Site site, Page page, String html) {
        log.debug("Calling method saveLemmas - LemmaService by {}", site.getName());
        Map<String, Integer> lemmaCounts = getLemmas(html);
        for (Map.Entry<String, Integer> entry : lemmaCounts.entrySet()) {
            String lemma = entry.getKey();
            Integer count = entry.getValue();
            service.saveLemma(site, page, lemma, count);
        }
    }

    public boolean filter(String word) {
        List<String> morphInfoList = morphology.getMorphInfo(word);
        for (String info : morphInfoList) {
            if (info.contains("СОЮЗ") || info.contains("МЕЖД") || info.contains("ПРЕДЛ") || info.contains("ЧАСТ")) {
                return true;
            }
        }
        return false;
    }

    public String cleanTags(String text) {
        return Jsoup.parse(text).text();
    }
}
