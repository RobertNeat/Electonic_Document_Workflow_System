package robert_neat.document_api.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.dto.administracja.WpisDziennikaZdarzen;
import robert_neat.document_api.entity.WpisDziennikaZdarzenEntity;
import robert_neat.document_api.mapper.AdministracjaMapper;
import robert_neat.document_api.repository.WpisDziennikaZdarzenRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** Tylko odczyt/filtrowanie istniejących wpisów - emisja wpisów przez inne serwisy (sprawy,
 * płatności, dokumenty) jest poza zakresem tego zadania (zob. kontrakt administracja.md, sekcja
 * "Dziennik zdarzeń"). Strefa UTC przyjęta konsekwentnie dla przeliczania dat granicznych `od`/`do`. */
@Service
@Transactional(readOnly = true)
public class AuditLogService {

    private final WpisDziennikaZdarzenRepository repository;

    public AuditLogService(WpisDziennikaZdarzenRepository repository) {
        this.repository = repository;
    }

    public List<WpisDziennikaZdarzen> lista(String typOperacji, LocalDate od, LocalDate doData) {
        OffsetDateTime odDataCzas = od == null ? null : od.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        // Górna granica wyłączna = początek dnia PO `do`, żeby `do` obejmowało cały wskazany dzień.
        OffsetDateTime doWylacznie = doData == null ? null : doData.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        Specification<WpisDziennikaZdarzenEntity> spec = Specification.allOf(
                WpisDziennikaZdarzenSpecifications.typOperacjiEquals(typOperacji),
                WpisDziennikaZdarzenSpecifications.dataCzasOd(odDataCzas),
                WpisDziennikaZdarzenSpecifications.dataCzasDo(doWylacznie)
        );
        return repository.findAll(spec).stream().map(AdministracjaMapper::toDto).toList();
    }
}
