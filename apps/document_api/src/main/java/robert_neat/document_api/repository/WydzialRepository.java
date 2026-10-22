package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.document_api.entity.WydzialEntity;

public interface WydzialRepository extends JpaRepository<WydzialEntity, String> {
}
