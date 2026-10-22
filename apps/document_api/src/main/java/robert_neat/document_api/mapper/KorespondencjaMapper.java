package robert_neat.document_api.mapper;

import robert_neat.document_api.dto.korespondencja.KanalWysylki;
import robert_neat.document_api.dto.korespondencja.Kierunek;
import robert_neat.document_api.dto.korespondencja.Przesylka;
import robert_neat.document_api.dto.korespondencja.StatusPrzesylki;
import robert_neat.document_api.dto.korespondencja.WpisStatusuPrzesylki;
import robert_neat.document_api.entity.PrzesylkaEntity;
import robert_neat.document_api.entity.WpisStatusuPrzesylkiEntity;

import java.util.List;

public final class KorespondencjaMapper {

    private KorespondencjaMapper() {
    }

    /** Konwersje string DB-value (CHECK constraint) <-> enum wire-format - nie @Enumerated(STRING),
     * analogicznie do DokumentyMapper.statusPodpisuFromDb. */
    public static KanalWysylki kanalFromDb(String wartosc) {
        for (KanalWysylki kanal : KanalWysylki.values()) {
            if (kanal.getWartosc().equals(wartosc)) {
                return kanal;
            }
        }
        throw new IllegalArgumentException("Nieznana wartość kanału wysyłki: " + wartosc);
    }

    public static StatusPrzesylki statusFromDb(String wartosc) {
        for (StatusPrzesylki status : StatusPrzesylki.values()) {
            if (status.getWartosc().equals(wartosc)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Nieznana wartość statusu przesyłki: " + wartosc);
    }

    public static Kierunek kierunekFromDb(String wartosc) {
        for (Kierunek kierunek : Kierunek.values()) {
            if (kierunek.getWartosc().equals(wartosc)) {
                return kierunek;
            }
        }
        throw new IllegalArgumentException("Nieznana wartość kierunku przesyłki: " + wartosc);
    }

    public static WpisStatusuPrzesylki toDto(WpisStatusuPrzesylkiEntity entity) {
        return new WpisStatusuPrzesylki(statusFromDb(entity.getStatus()), entity.getData());
    }

    /** historiaStatusow jest rozwiązywana przez serwis (zapytanie do wpis_statusu_przesylki)
     * i przekazywana jako gotowa lista - mapper pozostaje czysty. */
    public static Przesylka toDto(PrzesylkaEntity entity, List<WpisStatusuPrzesylki> historiaStatusow) {
        return new Przesylka(
                entity.getId(),
                entity.getSprawa().getId(),
                entity.getDokument().getId(),
                entity.getObywatel().getId(),
                kanalFromDb(entity.getKanal()),
                statusFromDb(entity.getStatus()),
                entity.getNumerReferencyjny(),
                entity.getDataNadania(),
                entity.getDataStatusu(),
                kierunekFromDb(entity.getKierunek()),
                historiaStatusow
        );
    }
}
