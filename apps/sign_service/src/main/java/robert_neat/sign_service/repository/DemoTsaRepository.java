package robert_neat.sign_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.sign_service.entity.DemoTsaEntity;

public interface DemoTsaRepository extends JpaRepository<DemoTsaEntity, String> {
}
