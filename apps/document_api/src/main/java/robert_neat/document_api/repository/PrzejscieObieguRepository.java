package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import robert_neat.document_api.entity.PrzejscieObieguEntity;

import java.util.List;
import java.util.Optional;

public interface PrzejscieObieguRepository extends JpaRepository<PrzejscieObieguEntity, String> {

    List<PrzejscieObieguEntity> findByDefinicjaObiegu_Id(String definicjaObieguId);

    // Derived-query parser myli się na nazwie "...AndZStanu" (wielka litera po podkreślniku
    // nested-path) - jawne JPQL jest tu bezpieczniejsze i czytelniejsze.
    @Query("SELECT p FROM PrzejscieObieguEntity p WHERE p.definicjaObiegu.id = :definicjaObieguId AND p.zstanu = :zStanu")
    Optional<PrzejscieObieguEntity> findZeStanu(@Param("definicjaObieguId") String definicjaObieguId, @Param("zStanu") String zStanu);
}
