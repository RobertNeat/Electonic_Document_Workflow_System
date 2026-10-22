package robert_neat.document_api.mapper;

import robert_neat.document_api.dto.akcjezbiorowe.StatusAkcjiZbiorowej;
import robert_neat.document_api.dto.akcjezbiorowe.StatusElementuAkcji;
import robert_neat.document_api.dto.akcjezbiorowe.StatusWykonaniaAkcji;
import robert_neat.document_api.dto.akcjezbiorowe.TypAkcjiZbiorowej;
import robert_neat.document_api.dto.akcjezbiorowe.TypCeluAkcjiZbiorowej;
import robert_neat.document_api.dto.akcjezbiorowe.WynikElementuAkcji;
import robert_neat.document_api.entity.AkcjaZbiorowaElementEntity;
import robert_neat.document_api.entity.AkcjaZbiorowaEntity;

import java.util.List;

/** typAkcji/typCelu: wartość DB-value JEST nazwą stałej enuma Java (np. "ZMIANA_STANU") - CHECK
 * constraint w 010-akcje-zbiorowe.xml wprost wylicza te nazwy wielkimi literami, a DTO nie mają
 * @JsonValue - stąd Enum.valueOf/name() wystarczają, bez tabeli konwersji jak w innych mapperach.
 * status/status elementu NATOMIAST mają @JsonValue z małoliterowym wire-formatem ('trwajaca'/
 * 'zakonczona', 'sukces'/'blad') zgodnym z CHECK - tu potrzeba jest konwersja przez getWartosc(). */
public final class AkcjeZbioroweMapper {

    private AkcjeZbioroweMapper() {
    }

    public static TypAkcjiZbiorowej typAkcjiFromDb(String wartosc) {
        return TypAkcjiZbiorowej.valueOf(wartosc);
    }

    public static String typAkcjiToDb(TypAkcjiZbiorowej typAkcji) {
        return typAkcji.name();
    }

    public static TypCeluAkcjiZbiorowej typCeluFromDb(String wartosc) {
        return TypCeluAkcjiZbiorowej.valueOf(wartosc);
    }

    public static String typCeluToDb(TypCeluAkcjiZbiorowej typCelu) {
        return typCelu.name();
    }

    public static StatusWykonaniaAkcji statusWykonaniaFromDb(String wartosc) {
        for (StatusWykonaniaAkcji status : StatusWykonaniaAkcji.values()) {
            if (status.getWartosc().equals(wartosc)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Nieznana wartość statusu wykonania akcji: " + wartosc);
    }

    public static StatusElementuAkcji statusElementuFromDb(String wartosc) {
        for (StatusElementuAkcji status : StatusElementuAkcji.values()) {
            if (status.getWartosc().equals(wartosc)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Nieznana wartość statusu elementu akcji: " + wartosc);
    }

    public static WynikElementuAkcji toDto(AkcjaZbiorowaElementEntity entity) {
        StatusElementuAkcji status = entity.getStatus() == null ? null : statusElementuFromDb(entity.getStatus());
        return new WynikElementuAkcji(entity.getIdCelu(), status, entity.getKomunikat());
    }

    public static StatusAkcjiZbiorowej toDto(AkcjaZbiorowaEntity entity, List<AkcjaZbiorowaElementEntity> elementy) {
        List<WynikElementuAkcji> wyniki = elementy.stream().map(AkcjeZbioroweMapper::toDto).toList();
        return new StatusAkcjiZbiorowej(
                entity.getId(),
                typAkcjiFromDb(entity.getTypAkcji()),
                typCeluFromDb(entity.getTypCelu()),
                statusWykonaniaFromDb(entity.getStatus()),
                entity.getPostep(),
                entity.getLacznie(),
                entity.getPrzetworzone(),
                entity.getBledne(),
                wyniki
        );
    }
}
