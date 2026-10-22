package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import robert_neat.document_api.entity.WpisDziennikaZdarzenEntity;

public interface WpisDziennikaZdarzenRepository
        extends JpaRepository<WpisDziennikaZdarzenEntity, String>, JpaSpecificationExecutor<WpisDziennikaZdarzenEntity> {
}
