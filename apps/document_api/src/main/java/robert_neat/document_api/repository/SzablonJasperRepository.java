package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.document_api.entity.SzablonJasperEntity;

import java.util.List;

public interface SzablonJasperRepository extends JpaRepository<SzablonJasperEntity, String> {

    List<SzablonJasperEntity> findByTypDokumentu_Id(String typDokumentuId);

    /** "Historia" szablonu = wszystkie wiersze o tej samej nazwie pliku + typie, malejąco po wersji
     * (schema nie modeluje osobnej tabeli historii zmian - zob. scoping note w raporcie). */
    List<SzablonJasperEntity> findByNazwaAndTypDokumentu_IdOrderByWersjaDesc(String nazwa, String typDokumentuId);
}
