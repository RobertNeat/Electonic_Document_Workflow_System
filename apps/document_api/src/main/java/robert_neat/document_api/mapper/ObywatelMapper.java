package robert_neat.document_api.mapper;

import robert_neat.document_api.dto.obywatele.AdresObywatela;
import robert_neat.document_api.dto.obywatele.Obywatel;
import robert_neat.document_api.dto.obywatele.TypObywatela;
import robert_neat.document_api.entity.AdresObywatelaEmbeddable;
import robert_neat.document_api.entity.ObywatelEntity;

public final class ObywatelMapper {

    private ObywatelMapper() {
    }

    public static Obywatel toDto(ObywatelEntity entity) {
        AdresObywatelaEmbeddable a = entity.getAdres();
        AdresObywatela adres = new AdresObywatela(
                a == null ? null : a.getUlica(),
                a == null ? null : a.getNumerDomu(),
                a == null ? null : a.getNumerLokalu(),
                a == null ? null : a.getKodPocztowy(),
                a == null ? null : a.getMiejscowosc()
        );
        return new Obywatel(
                entity.getId(),
                toTyp(entity.getTyp()),
                entity.getImie(),
                entity.getNazwisko(),
                entity.getNazwaPodmiotu(),
                entity.getPesel(),
                entity.getNip(),
                entity.getAdresDoreczenEDE(),
                entity.getEmail(),
                entity.getTelefon(),
                adres
        );
    }

    private static TypObywatela toTyp(String dbValue) {
        for (TypObywatela t : TypObywatela.values()) {
            if (t.getWartosc().equals(dbValue)) {
                return t;
            }
        }
        throw new IllegalStateException("Nieznana wartość typ obywatela w bazie: " + dbValue);
    }
}
