package robert_neat.dms_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import robert_neat.dms_service.entity.WersjaDokumentuDmsEntity;

import java.util.List;
import java.util.Optional;

public interface WersjaDokumentuDmsRepository extends JpaRepository<WersjaDokumentuDmsEntity, String> {

    List<WersjaDokumentuDmsEntity> findByDokumentIdOrderByWersjaAsc(String dokumentId);

    Optional<WersjaDokumentuDmsEntity> findByDokumentIdAndWersja(String dokumentId, int wersja);

    @Query("select max(w.wersja) from WersjaDokumentuDmsEntity w where w.dokumentId = :dokumentId")
    Optional<Integer> znajdzMaksWersje(@Param("dokumentId") String dokumentId);
}
