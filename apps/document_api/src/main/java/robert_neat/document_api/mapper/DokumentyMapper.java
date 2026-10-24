package robert_neat.document_api.mapper;

import robert_neat.document_api.dto.dokumenty.Dokument;
import robert_neat.document_api.dto.dokumenty.DokumentListElement;
import robert_neat.document_api.dto.dokumenty.StatusPodpisu;
import robert_neat.document_api.dto.dokumenty.SzablonJasper;
import robert_neat.document_api.dto.dokumenty.TypDokumentu;
import robert_neat.document_api.dto.dokumenty.WersjaDokumentu;
import robert_neat.document_api.entity.DokumentEntity;
import robert_neat.document_api.entity.SzablonJasperEntity;
import robert_neat.document_api.entity.TypDokumentuEntity;
import robert_neat.document_api.entity.WersjaDokumentuEntity;

public final class DokumentyMapper {

    private DokumentyMapper() {
    }

    /** Konwersja string DB-value (CHECK constraint) na enum wire-format - nie @Enumerated(STRING),
     * bo to zapisałoby nazwę stałej Java ('BRAK') zamiast wartości DB ('brak'). */
    public static StatusPodpisu statusPodpisuFromDb(String wartosc) {
        for (StatusPodpisu status : StatusPodpisu.values()) {
            if (status.getWartosc().equals(wartosc)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Nieznana wartość statusu podpisu: " + wartosc);
    }

    public static TypDokumentu toDto(TypDokumentuEntity entity) {
        return new TypDokumentu(entity.getId(), entity.getNazwa(), entity.getOpis());
    }

    public static SzablonJasper toDto(SzablonJasperEntity entity) {
        return new SzablonJasper(
                entity.getId(),
                entity.getTypDokumentu().getId(),
                entity.getNazwa(),
                entity.getWersja(),
                entity.getDataWgrania(),
                entity.getAutor()
        );
    }

    public static Dokument toDto(DokumentEntity entity) {
        return new Dokument(
                entity.getId(),
                entity.getSprawa().getId(),
                entity.getTypDokumentu().getId(),
                entity.getNazwa(),
                entity.getAktualnaWersja(),
                entity.getDataUtworzenia()
        );
    }

    public static WersjaDokumentu toDto(WersjaDokumentuEntity entity) {
        return new WersjaDokumentu(
                entity.getId(),
                entity.getDokument().getId(),
                entity.getWersja(),
                entity.isPodpisana(),
                entity.getDataUtworzenia(),
                entity.getAutor(),
                statusPodpisuFromDb(entity.getStatusPodpisu())
        );
    }

    /** znakSprawy, statusPodpisuAktualnej i typDokumentuNazwa są rozwiązywane przez serwis (join
     * sprawa/wersja_dokumentu/typ_dokumentu) i przekazywane jako proste wartości - mapper pozostaje
     * czysty, nie odpytuje repozytoriów. */
    public static DokumentListElement toListElement(DokumentEntity entity, String znakSprawy,
                                                      StatusPodpisu statusPodpisuAktualnej, String typDokumentuNazwa) {
        return new DokumentListElement(
                entity.getId(),
                entity.getSprawa().getId(),
                entity.getTypDokumentu().getId(),
                entity.getNazwa(),
                entity.getAktualnaWersja(),
                entity.getDataUtworzenia(),
                znakSprawy,
                statusPodpisuAktualnej,
                typDokumentuNazwa
        );
    }
}
