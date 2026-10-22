package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.document_api.entity.TypDokumentuEntity;

public interface TypDokumentuRepository extends JpaRepository<TypDokumentuEntity, String> {
}
