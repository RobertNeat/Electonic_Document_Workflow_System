package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import robert_neat.document_api.entity.WplataEntity;

import java.util.List;

public interface WplataRepository extends JpaRepository<WplataEntity, String>, JpaSpecificationExecutor<WplataEntity> {

    List<WplataEntity> findByNaleznosc_Id(String naleznoscId);
}
