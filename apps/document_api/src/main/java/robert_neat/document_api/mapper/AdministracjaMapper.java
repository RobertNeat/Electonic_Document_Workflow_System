package robert_neat.document_api.mapper;

import robert_neat.document_api.dto.administracja.DostawcaWysylki;
import robert_neat.document_api.dto.administracja.PozycjaJrwa;
import robert_neat.document_api.dto.administracja.RolaUzytkownika;
import robert_neat.document_api.dto.administracja.StawkaOplaty;
import robert_neat.document_api.dto.administracja.Stanowisko;
import robert_neat.document_api.dto.administracja.TypDostawcyWysylki;
import robert_neat.document_api.dto.administracja.TypOperacjiAudytu;
import robert_neat.document_api.dto.administracja.Uzytkownik;
import robert_neat.document_api.dto.administracja.Wydzial;
import robert_neat.document_api.dto.administracja.WpisDziennikaZdarzen;
import robert_neat.document_api.dto.sprawy.KategoriaArchiwalna;
import robert_neat.document_api.entity.DostawcaWysylkiEntity;
import robert_neat.document_api.entity.PozycjaJrwaEntity;
import robert_neat.document_api.entity.StanowiskoEntity;
import robert_neat.document_api.entity.StawkaOplatyEntity;
import robert_neat.document_api.entity.UzytkownikEntity;
import robert_neat.document_api.entity.WpisDziennikaZdarzenEntity;
import robert_neat.document_api.entity.WydzialEntity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class AdministracjaMapper {

    private AdministracjaMapper() {
    }

    // --- uzytkownik / rola ---
    // RolaUzytkownika.getWartosc() ma wire-format = DB-value (CHECK ck_uzytkownik_rola, małe litery),
    // więc konwersja jest wprost przez wartość, tak jak typDostawcyFromDb/typOperacjiFromDb poniżej.

    public static String rolaToDb(RolaUzytkownika rola) {
        return rola.getWartosc();
    }

    public static RolaUzytkownika rolaFromDb(String dbValue) {
        for (RolaUzytkownika rola : RolaUzytkownika.values()) {
            if (rola.getWartosc().equals(dbValue)) {
                return rola;
            }
        }
        throw new IllegalArgumentException("Nieznana wartość roli użytkownika: " + dbValue);
    }

    public static Uzytkownik toDto(UzytkownikEntity entity) {
        List<RolaUzytkownika> role = entity.getRole().stream()
                .map(AdministracjaMapper::rolaFromDb)
                .sorted()
                .toList();
        return new Uzytkownik(
                entity.getId(),
                entity.getLogin(),
                entity.getImie(),
                entity.getNazwisko(),
                entity.getEmail(),
                role,
                entity.getWydzial(),
                entity.getStanowisko()
        );
    }

    public static Set<String> roleToDb(List<RolaUzytkownika> role) {
        if (role == null) {
            return Set.of();
        }
        return role.stream().map(AdministracjaMapper::rolaToDb).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    // --- struktura organizacyjna ---

    public static Stanowisko toDto(StanowiskoEntity entity) {
        return new Stanowisko(entity.getId(), entity.getNazwa());
    }

    public static Wydzial toDto(WydzialEntity entity, List<Stanowisko> stanowiska) {
        return new Wydzial(entity.getId(), entity.getNazwa(), entity.getKierownik(), stanowiska);
    }

    // --- slowniki ---

    public static PozycjaJrwa toDto(PozycjaJrwaEntity entity) {
        return new PozycjaJrwa(
                entity.getSymbol(),
                entity.getHaslo(),
                KategoriaArchiwalna.valueOf(entity.getKategoriaArchiwalna())
        );
    }

    public static DostawcaWysylki toDto(DostawcaWysylkiEntity entity) {
        return new DostawcaWysylki(
                entity.getId(),
                entity.getNazwa(),
                typDostawcyFromDb(entity.getTyp()),
                entity.getOperatorPocztowy(),
                entity.getEmail(),
                entity.isAktywny()
        );
    }

    public static TypDostawcyWysylki typDostawcyFromDb(String dbValue) {
        for (TypDostawcyWysylki typ : TypDostawcyWysylki.values()) {
            if (typ.getWartosc().equals(dbValue)) {
                return typ;
            }
        }
        throw new IllegalArgumentException("Nieznana wartość typu dostawcy wysyłki: " + dbValue);
    }

    public static StawkaOplaty toDto(StawkaOplatyEntity entity) {
        return new StawkaOplaty(entity.getId(), entity.getNazwa(), entity.getKwota());
    }

    // --- dziennik zdarzen ---

    public static WpisDziennikaZdarzen toDto(WpisDziennikaZdarzenEntity entity) {
        return new WpisDziennikaZdarzen(
                entity.getId(),
                entity.getDataCzas(),
                entity.getUzytkownik(),
                typOperacjiFromDb(entity.getTypOperacji()),
                entity.getObiekt(),
                entity.getSzczegoly()
        );
    }

    public static TypOperacjiAudytu typOperacjiFromDb(String dbValue) {
        for (TypOperacjiAudytu typ : TypOperacjiAudytu.values()) {
            if (typ.getWartosc().equals(dbValue)) {
                return typ;
            }
        }
        throw new IllegalArgumentException("Nieznana wartość typu operacji audytu: " + dbValue);
    }
}
