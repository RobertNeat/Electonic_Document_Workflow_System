package robert_neat.document_api.mapper;

import robert_neat.document_api.dto.platnosci.Naleznosc;
import robert_neat.document_api.dto.platnosci.NaleznoscListElement;
import robert_neat.document_api.dto.platnosci.StatusRozliczenia;
import robert_neat.document_api.dto.platnosci.Wplata;
import robert_neat.document_api.entity.NaleznoscEntity;
import robert_neat.document_api.entity.ObywatelEntity;
import robert_neat.document_api.entity.WplataEntity;

public final class PlatnosciMapper {

    private PlatnosciMapper() {
    }

    /** Konwersja string DB-value (CHECK constraint) na enum wire-format - nie @Enumerated(STRING),
     * bo to zapisałoby nazwę stałej Java ('NIEROZLICZONA') zamiast wartości DB ('nierozliczona'). */
    public static StatusRozliczenia statusRozliczeniaFromDb(String wartosc) {
        for (StatusRozliczenia status : StatusRozliczenia.values()) {
            if (status.getWartosc().equals(wartosc)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Nieznana wartość statusu rozliczenia: " + wartosc);
    }

    /** Nazwa wyświetlana obywatela/podmiotu - podmiot ma nazwaPodmiotu, osoba fizyczna imię+nazwisko. */
    public static String obywatelNazwa(ObywatelEntity obywatel) {
        if ("podmiot".equals(obywatel.getTyp())) {
            return obywatel.getNazwaPodmiotu();
        }
        return (obywatel.getImie() == null ? "" : obywatel.getImie() + " ") +
                (obywatel.getNazwisko() == null ? "" : obywatel.getNazwisko());
    }

    public static Naleznosc toDto(NaleznoscEntity entity) {
        return new Naleznosc(
                entity.getId(),
                entity.getSprawa().getId(),
                entity.getObywatel().getId(),
                entity.getNumerReferencyjny(),
                entity.getTytul(),
                entity.getKwota(),
                entity.getKwotaRozliczona(),
                entity.getTerminPlatnosci(),
                statusRozliczeniaFromDb(entity.getStatus())
        );
    }

    /** znakSprawy i obywatelNazwa są rozwiązywane przez serwis (join sprawa/obywatel już załadowany
     * przez asocjację JPA) i przekazywane jako proste wartości - mapper pozostaje czysty. */
    public static NaleznoscListElement toListElement(NaleznoscEntity entity, String znakSprawy, String obywatelNazwa) {
        return new NaleznoscListElement(
                entity.getId(),
                entity.getSprawa().getId(),
                entity.getObywatel().getId(),
                entity.getNumerReferencyjny(),
                entity.getTytul(),
                entity.getKwota(),
                entity.getKwotaRozliczona(),
                entity.getTerminPlatnosci(),
                statusRozliczeniaFromDb(entity.getStatus()),
                znakSprawy,
                obywatelNazwa
        );
    }

    public static Wplata toDto(WplataEntity entity) {
        return new Wplata(
                entity.getId(),
                entity.getNaleznosc() == null ? null : entity.getNaleznosc().getId(),
                entity.getKwota(),
                entity.getData(),
                entity.getTytul(),
                entity.getKontrahent(),
                entity.isDopasowanaAutomatycznie()
        );
    }
}
