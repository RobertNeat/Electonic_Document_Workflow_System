package robert_neat.sign_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.sign_service.entity.UniewaznienieEntity;

import java.util.List;

public interface UniewaznienieRepository extends JpaRepository<UniewaznienieEntity, String> {

    List<UniewaznienieEntity> findByCertyfikat_Id(String certyfikatId);
}
