package robert_neat.document_api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import robert_neat.document_api.entity.SprawaEntity;

import java.util.List;

public interface SprawaRepository extends JpaRepository<SprawaEntity, String>, JpaSpecificationExecutor<SprawaEntity> {

    Page<SprawaEntity> findByObywatel_Id(String obywatelId, Pageable pageable);

    /** Znaki spraw danego wydziału/jrwa/roku, do wyliczenia kolejnego numeru (parsowanie max+1 po stronie serwisu). */
    @Query("SELECT s.znakSprawy FROM SprawaEntity s WHERE s.znakSprawy LIKE :prefix")
    List<String> findZnakiSprawyByPrefix(@Param("prefix") String prefix);
}
