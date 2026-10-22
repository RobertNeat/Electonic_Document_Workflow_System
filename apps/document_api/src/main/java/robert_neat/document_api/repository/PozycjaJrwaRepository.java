package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.document_api.entity.PozycjaJrwaEntity;

public interface PozycjaJrwaRepository extends JpaRepository<PozycjaJrwaEntity, String> {
}
