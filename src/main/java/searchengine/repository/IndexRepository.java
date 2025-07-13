package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;
import java.util.Optional;

public interface IndexRepository extends JpaRepository<Index, Integer> {
    Optional<Index> findByLemmaIdAndPageId(Integer lemmaId, Integer pageId);

    List<Index> findAllByLemmaId(Integer lemmaId);
    List<Index> findAllByPageId(Integer pageId);

    void deleteAllByPageId(Integer id);

    @Modifying
    @Transactional
    @Query("DELETE FROM Index i WHERE i.page = :page")
    void deleteAllByPage(@Param("page") Page page);
}
