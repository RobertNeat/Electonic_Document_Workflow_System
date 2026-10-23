package robert_neat.sign_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.sign_service.entity.WystawionyPodpisEntity;

public interface WystawionyPodpisRepository extends JpaRepository<WystawionyPodpisEntity, String> {
}
