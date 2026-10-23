package robert_neat.delivery_mock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.delivery_mock.entity.WpisStatusuDostawyEntity;

import java.util.List;

public interface WpisStatusuDostawyRepository extends JpaRepository<WpisStatusuDostawyEntity, String> {

    List<WpisStatusuDostawyEntity> findByPrzesylkaDostawyIdOrderByDataAsc(String przesylkaDostawyId);
}
