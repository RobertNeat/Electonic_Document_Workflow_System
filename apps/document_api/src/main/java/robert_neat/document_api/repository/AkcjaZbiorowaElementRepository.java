package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.document_api.entity.AkcjaZbiorowaElementEntity;

import java.util.List;

public interface AkcjaZbiorowaElementRepository extends JpaRepository<AkcjaZbiorowaElementEntity, String> {

    List<AkcjaZbiorowaElementEntity> findByAkcjaZbiorowa_Id(String akcjaZbiorowaId);
}
