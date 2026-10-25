package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.document_api.entity.SzablonJasperEntity;

import java.util.List;
import java.util.Optional;

public interface SzablonJasperRepository extends JpaRepository<SzablonJasperEntity, String> {

    List<SzablonJasperEntity> findByTypDokumentu_Id(String typDokumentuId);

    /** "Historia" szablonu = wszystkie wiersze o tej samej nazwie pliku + typie, malejąco po wersji
     * (schema nie modeluje osobnej tabeli historii zmian - zob. scoping note w raporcie). */
    List<SzablonJasperEntity> findByNazwaAndTypDokumentu_IdOrderByWersjaDesc(String nazwa, String typDokumentuId);

    /** Najnowsza wersja szablonu DANEGO TYPU DOKUMENTU, bez względu na nazwę pliku - używane tam,
     * gdzie wybór szablonu idzie po typie dokumentu (np. wezwanie do zapłaty), nie po konkretnej
     * nazwie pliku, żeby zmiana pliku .jrxml nie wymagała zmiany kodu wołającego. */
    Optional<SzablonJasperEntity> findFirstByTypDokumentu_IdOrderByWersjaDesc(String typDokumentuId);
}
