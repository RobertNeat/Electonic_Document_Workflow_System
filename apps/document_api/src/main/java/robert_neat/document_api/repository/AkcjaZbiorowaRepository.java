package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.document_api.entity.AkcjaZbiorowaEntity;

public interface AkcjaZbiorowaRepository extends JpaRepository<AkcjaZbiorowaEntity, String> {
}
