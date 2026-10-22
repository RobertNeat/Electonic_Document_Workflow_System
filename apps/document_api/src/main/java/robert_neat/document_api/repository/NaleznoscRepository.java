package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import robert_neat.document_api.entity.NaleznoscEntity;

public interface NaleznoscRepository extends JpaRepository<NaleznoscEntity, String>, JpaSpecificationExecutor<NaleznoscEntity> {
}
