package robert_neat.document_api.mapper;

import robert_neat.document_api.dto.wymiana.KierunekWymiany;
import robert_neat.document_api.dto.wymiana.StatusWymiany;
import robert_neat.document_api.dto.wymiana.WpisHistoriiWymiany;
import robert_neat.document_api.entity.WpisHistoriiWymianyEntity;

public final class WymianaMapper {

    private WymianaMapper() {
    }

    /** Konwersja string DB-value (CHECK constraint) na enum wire-format - nie @Enumerated(STRING),
     * analogicznie do PlatnosciMapper.statusRozliczeniaFromDb. */
    public static KierunekWymiany kierunekFromDb(String wartosc) {
        for (KierunekWymiany kierunek : KierunekWymiany.values()) {
            if (kierunek.getWartosc().equals(wartosc)) {
                return kierunek;
            }
        }
        throw new IllegalArgumentException("Nieznana wartość kierunku wymiany: " + wartosc);
    }

    public static StatusWymiany statusFromDb(String wartosc) {
        for (StatusWymiany status : StatusWymiany.values()) {
            if (status.getWartosc().equals(wartosc)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Nieznana wartość statusu wymiany: " + wartosc);
    }

    public static WpisHistoriiWymiany toDto(WpisHistoriiWymianyEntity entity) {
        return new WpisHistoriiWymiany(
                entity.getId(),
                entity.getNazwaPliku(),
                kierunekFromDb(entity.getKierunek()),
                entity.getData(),
                statusFromDb(entity.getStatus()),
                entity.getLiczbaRekordow(),
                entity.getOpis()
        );
    }
}
