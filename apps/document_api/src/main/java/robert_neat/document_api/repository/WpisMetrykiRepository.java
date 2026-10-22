package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.document_api.entity.WpisMetrykiEntity;

import java.util.List;

public interface WpisMetrykiRepository extends JpaRepository<WpisMetrykiEntity, String> {

    List<WpisMetrykiEntity> findBySprawa_IdOrderByDataAsc(String sprawaId);
}
