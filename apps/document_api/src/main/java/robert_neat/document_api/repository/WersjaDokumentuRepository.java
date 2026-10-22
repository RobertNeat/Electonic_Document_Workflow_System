package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.document_api.entity.WersjaDokumentuEntity;

import java.util.List;
import java.util.Optional;

public interface WersjaDokumentuRepository extends JpaRepository<WersjaDokumentuEntity, String> {

    List<WersjaDokumentuEntity> findByDokument_IdOrderByWersjaDesc(String dokumentId);

    Optional<WersjaDokumentuEntity> findByDokument_IdAndWersja(String dokumentId, int wersja);

    /** Do mapowania zdenormalizowanych list (DokumentListElement.statusPodpisuAktualnej) -
     * jedno zapytanie po wielu dokumentach + filtrowanie w serwisie do wersji == aktualnaWersja
     * danego dokumentu (unikamy N+1 bez dedykowanego zapytania z MAX per-wiersz). */
    List<WersjaDokumentuEntity> findByDokument_IdIn(List<String> dokumentIds);
}
