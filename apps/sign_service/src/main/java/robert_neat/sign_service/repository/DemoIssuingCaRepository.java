package robert_neat.sign_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.sign_service.entity.DemoIssuingCaEntity;

public interface DemoIssuingCaRepository extends JpaRepository<DemoIssuingCaEntity, String> {
}
