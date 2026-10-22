package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import robert_neat.document_api.entity.DokumentEntity;

import java.util.List;

public interface DokumentRepository extends JpaRepository<DokumentEntity, String>, JpaSpecificationExecutor<DokumentEntity> {

    /** Dokumenty danej sprawy, do wyszukania (w serwisie akcji zbiorowych WYSYLKA_PISM) najnowszego
     * podpisanego dokumentu - filtrowanie po aktualnej+podpisanej wersji odbywa się w serwisie. */
    List<DokumentEntity> findBySprawa_Id(String sprawaId);
}
