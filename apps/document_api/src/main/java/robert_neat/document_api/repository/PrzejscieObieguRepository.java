package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import robert_neat.document_api.entity.PrzejscieObieguEntity;

import java.util.List;

public interface PrzejscieObieguRepository extends JpaRepository<PrzejscieObieguEntity, String> {

    List<PrzejscieObieguEntity> findByDefinicjaObiegu_Id(String definicjaObieguId);

    // Derived-query parser myli się na nazwie "...AndZStanu" (wielka litera po podkreślniku
    // nested-path) - jawne JPQL jest tu bezpieczniejsze i czytelniejsze.
    // Lista (nie Optional) - przyszłe rozgałęzienie obiegu (więcej niż jedno przejście z tego
    // samego stanu) jest poprawnym przypadkiem, nie błędem.
    @Query("SELECT p FROM PrzejscieObieguEntity p WHERE p.definicjaObiegu.id = :definicjaObieguId AND p.zstanu = :zStanu")
    List<PrzejscieObieguEntity> findZeStanu(@Param("definicjaObieguId") String definicjaObieguId, @Param("zStanu") String zStanu);
}
