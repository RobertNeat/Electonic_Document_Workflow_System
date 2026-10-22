package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.document_api.entity.StawkaOplatyEntity;

public interface StawkaOplatyRepository extends JpaRepository<StawkaOplatyEntity, String> {
}
