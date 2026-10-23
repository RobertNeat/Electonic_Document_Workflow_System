package robert_neat.dms_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import robert_neat.dms_service.entity.SzablonJasperEntity;

import java.util.Optional;

public interface SzablonJasperRepository extends JpaRepository<SzablonJasperEntity, String> {

    @Query("select max(s.wersja) from SzablonJasperEntity s where s.typDokumentuId = :typDokumentuId and s.nazwa = :nazwa")
    Optional<Integer> znajdzMaksWersje(@Param("typDokumentuId") String typDokumentuId, @Param("nazwa") String nazwa);
}
