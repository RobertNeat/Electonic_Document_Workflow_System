package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.document_api.entity.StanowiskoEntity;

import java.util.List;

public interface StanowiskoRepository extends JpaRepository<StanowiskoEntity, String> {

    List<StanowiskoEntity> findByWydzial_Id(String wydzialId);
}
