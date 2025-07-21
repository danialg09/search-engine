package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Optional<Page> findByPath(String path);

    void deleteAllBySiteId(Integer id);

    List<Page> findAllBySiteId(Integer id);

    Long countPageBySiteId(Integer id);

    Optional<Page> findByPathAndSite(String path, Site site);

    @Query("SELECT p.path FROM Page p WHERE p.path IN :paths")
    List<String> findPathsByPathIn(List<String> paths);
}
