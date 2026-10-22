package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.document_api.entity.WpisHistoriiWymianyEntity;

import java.util.List;

public interface WpisHistoriiWymianyRepository extends JpaRepository<WpisHistoriiWymianyEntity, String> {

    List<WpisHistoriiWymianyEntity> findByTypWymianyOrderByDataDesc(String typWymiany);
}
