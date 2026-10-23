package robert_neat.sign_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.sign_service.entity.DemoCaEntity;

public interface DemoCaRepository extends JpaRepository<DemoCaEntity, String> {
}
