package robert_neat.delivery_mock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.delivery_mock.entity.PrzesylkaDostawyEntity;

public interface PrzesylkaDostawyRepository extends JpaRepository<PrzesylkaDostawyEntity, String> {

    boolean existsByNumerReferencyjny(String numerReferencyjny);
}
