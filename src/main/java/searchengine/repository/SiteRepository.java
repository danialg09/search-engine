package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    Optional<SiteEntity> findByUrl(String url);
}
