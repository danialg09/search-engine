package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Modifying
    @Query("UPDATE Lemma l SET l.frequency = l.frequency + 1 WHERE l.id = :id")
    void incrementFrequencyById(@Param("id") Integer id);

    @Modifying
    @Query("UPDATE Lemma l SET l.frequency = l.frequency - 1 WHERE l.id = :id")
    void decrementFrequencyById(@Param("id") Integer id);

    @Modifying
    @Query("DELETE FROM Lemma l WHERE l.frequency <= 0")
    void deleteAllByFrequencyZero();


    List<Lemma> findAllByLemma(String lemma);

    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);

    void deleteAllBySiteId(Integer siteId);

    Long countLemmaBySiteId(Integer siteId);

}
