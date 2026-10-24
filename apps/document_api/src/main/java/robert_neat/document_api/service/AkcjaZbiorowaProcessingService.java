package robert_neat.document_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.dto.akcjezbiorowe.ParametryAktualizacjaRol;
import robert_neat.document_api.dto.akcjezbiorowe.ParametryGenerowanieDokumentow;
import robert_neat.document_api.dto.akcjezbiorowe.ParametryWysylkaPism;
import robert_neat.document_api.dto.akcjezbiorowe.ParametryZmianaStanu;
import robert_neat.document_api.dto.akcjezbiorowe.TypAkcjiZbiorowej;
import robert_neat.document_api.dto.administracja.RolaUzytkownika;
import robert_neat.document_api.dto.administracja.TypOperacjiAudytu;
import robert_neat.document_api.dto.dokumenty.StatusPodpisu;
import robert_neat.document_api.dto.korespondencja.UtworzPrzesylkeRequest;
import robert_neat.document_api.dto.sprawy.ZmienStanSprawyRequest;
import robert_neat.document_api.entity.AkcjaZbiorowaElementEntity;
import robert_neat.document_api.entity.AkcjaZbiorowaEntity;
import robert_neat.document_api.entity.DokumentEntity;
import robert_neat.document_api.entity.SprawaEntity;
import robert_neat.document_api.entity.SzablonJasperEntity;
import robert_neat.document_api.entity.UzytkownikEntity;
import robert_neat.document_api.entity.WersjaDokumentuEntity;
import robert_neat.document_api.exception.BadRequestException;
import robert_neat.document_api.exception.IllegalStateTransitionException;
import robert_neat.document_api.exception.NotFoundException;
import robert_neat.document_api.mapper.AdministracjaMapper;
import robert_neat.document_api.repository.AkcjaZbiorowaElementRepository;
import robert_neat.document_api.repository.AkcjaZbiorowaRepository;
import robert_neat.document_api.repository.DokumentRepository;
import robert_neat.document_api.repository.SprawaRepository;
import robert_neat.document_api.repository.SzablonJasperRepository;
import robert_neat.document_api.repository.UzytkownikRepository;
import robert_neat.document_api.repository.WersjaDokumentuRepository;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Logika "per-element" wołana z AkcjaZbiorowaConsumer. przetworzElement samo NIE jest
 * transakcyjne - delegacja do `self` (self-injected proxy, @Lazy żeby uniknąć cyklu w konstruktorze)
 * zapewnia, że wykonajOperacjeDomenowa i zapiszWynikElementu przechodzą przez pełny AOP-proxy Spring
 * i commitują się jako DWIE NIEZALEŻNE transakcje (REQUIRES_NEW) - zob. komentarz przy
 * przetworzElement, czemu rozdzielenie na dwie transakcje (a nie jedna wspólna) jest konieczne. */
@Service
public class AkcjaZbiorowaProcessingService {

    private final AkcjaZbiorowaRepository akcjaZbiorowaRepository;
    private final AkcjaZbiorowaElementRepository akcjaZbiorowaElementRepository;
    private final SprawaService sprawaService;
    private final KorespondencjaService korespondencjaService;
    private final SprawaRepository sprawaRepository;
    private final SzablonJasperRepository szablonJasperRepository;
    private final DokumentRepository dokumentRepository;
    private final WersjaDokumentuRepository wersjaDokumentuRepository;
    private final UzytkownikRepository uzytkownikRepository;
    private final JsonMapper jsonMapper;
    private final AuditLogService auditLogService;

    private AkcjaZbiorowaProcessingService self;

    public AkcjaZbiorowaProcessingService(AkcjaZbiorowaRepository akcjaZbiorowaRepository,
                                           AkcjaZbiorowaElementRepository akcjaZbiorowaElementRepository,
                                           SprawaService sprawaService,
                                           KorespondencjaService korespondencjaService,
                                           SprawaRepository sprawaRepository,
                                           SzablonJasperRepository szablonJasperRepository,
                                           DokumentRepository dokumentRepository,
                                           WersjaDokumentuRepository wersjaDokumentuRepository,
                                           UzytkownikRepository uzytkownikRepository,
                                           JsonMapper jsonMapper,
                                           AuditLogService auditLogService) {
        this.akcjaZbiorowaRepository = akcjaZbiorowaRepository;
        this.akcjaZbiorowaElementRepository = akcjaZbiorowaElementRepository;
        this.sprawaService = sprawaService;
        this.korespondencjaService = korespondencjaService;
        this.sprawaRepository = sprawaRepository;
        this.szablonJasperRepository = szablonJasperRepository;
        this.dokumentRepository = dokumentRepository;
        this.wersjaDokumentuRepository = wersjaDokumentuRepository;
        this.uzytkownikRepository = uzytkownikRepository;
        this.jsonMapper = jsonMapper;
        this.auditLogService = auditLogService;
    }

    /** Wstrzyknięcie samego siebie (przez proxy) - @Lazy, żeby ApplicationContext mógł dokończyć
     * tworzenie tego beana przed rozwiązaniem cyklicznej referencji na jego własny proxy. */
    @Autowired
    public void setSelf(@Lazy AkcjaZbiorowaProcessingService self) {
        this.self = self;
    }

    /** Przetwarza jeden element. NIE jest @Transactional samo - wywołuje domenową operację i zapis
     * wyniku jako DWIE ODRĘBNE transakcje (przez self-injected proxy `self`, bo wywołanie przez
     * `this` obchodzi przechwytywanie AOP). To jest konieczne, nie tylko stylistyczne: operacje
     * domenowe (SprawaService.zmienStan, KorespondencjaService.nadajPismo) mają własne @Transactional
     * (REQUIRED) - wołane z wnętrza JEDNEJ transakcji "przetworzElement" dołączyłyby się do niej, a
     * wyjątek typu NotFoundException/IllegalStateTransitionException wewnątrz ich @Transactional
     * boundary oznaczyłby TĘ SAMĄ transakcję jako rollback-only, mimo złapania wyjątku w try/catch
     * tutaj - przy próbie zapisania statusu "blad" dostalibyśmy UnexpectedRollbackException (tak
     * faktycznie się stało przy pierwszym uruchomieniu testów - zob. komentarz w raporcie). Stąd
     * wykonanieOperacjiDomenowej ma WŁASNĄ transakcję (REQUIRES_NEW), niezależną od tej, w której
     * zapisywany jest wynik (zapiszWynikElementu, też REQUIRES_NEW) - błąd (w tym rollback-only)
     * pierwszej nie ma żadnego wpływu na drugą. */
    public void przetworzElement(String elementId, TypAkcjiZbiorowej typAkcji, String parametryJson, String osoba) {
        AkcjaZbiorowaElementEntity element = akcjaZbiorowaElementRepository.findById(elementId)
                .orElseThrow(() -> new NotFoundException("Element akcji zbiorowej o id=" + elementId + " nie istnieje"));
        String idCelu = element.getIdCelu();

        boolean sukces;
        String komunikat = null;
        try {
            self.wykonajOperacjeDomenowa(typAkcji, idCelu, parametryJson, osoba);
            sukces = true;
        } catch (RuntimeException ex) {
            sukces = false;
            komunikat = ex.getMessage();
        }

        self.zapiszWynikElementu(elementId, sukces, komunikat);
    }

    /** Wykonuje właściwą operację domenową we własnej transakcji - jeśli operacja domenowa rzuci
     * wyjątek i oznaczy TĘ transakcję jako rollback-only, dotyczy to wyłącznie tej transakcji
     * (REQUIRES_NEW), nie transakcji zapisującej wynik elementu. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void wykonajOperacjeDomenowa(TypAkcjiZbiorowej typAkcji, String idCelu, String parametryJson, String osoba) {
        wykonaj(typAkcji, idCelu, parametryJson, osoba);
    }

    /** Zapisuje status/komunikat elementu i aktualizuje liczniki rodzica we własnej, zawsze czystej
     * transakcji (REQUIRES_NEW) - niezależnej od ewentualnego rollback-only w wykonajOperacjeDomenowa. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void zapiszWynikElementu(String elementId, boolean sukces, String komunikat) {
        AkcjaZbiorowaElementEntity element = akcjaZbiorowaElementRepository.findById(elementId)
                .orElseThrow(() -> new NotFoundException("Element akcji zbiorowej o id=" + elementId + " nie istnieje"));

        element.setStatus(sukces ? "sukces" : "blad");
        element.setKomunikat(komunikat);
        akcjaZbiorowaElementRepository.save(element);

        AkcjaZbiorowaEntity akcja = element.getAkcjaZbiorowa();
        akcja.setPrzetworzone(sukces ? akcja.getPrzetworzone() + 1 : akcja.getPrzetworzone());
        akcja.setBledne(sukces ? akcja.getBledne() : akcja.getBledne() + 1);
        int gotowe = akcja.getPrzetworzone() + akcja.getBledne();
        akcja.setPostep(akcja.getLacznie() == 0 ? 100 : (gotowe * 100) / akcja.getLacznie());
        akcjaZbiorowaRepository.save(akcja);
    }

    /** Oznacza akcję jako zakończoną - wołane po przetworzeniu wszystkich elementów przez konsumenta. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void zakonczAkcje(String akcjaId) {
        AkcjaZbiorowaEntity akcja = akcjaZbiorowaRepository.findById(akcjaId)
                .orElseThrow(() -> new NotFoundException("Akcja zbiorowa o id=" + akcjaId + " nie istnieje"));
        akcja.setStatus("zakonczona");
        akcjaZbiorowaRepository.save(akcja);
    }

    private void wykonaj(TypAkcjiZbiorowej typAkcji, String idCelu, String parametryJson, String osoba) {
        switch (typAkcji) {
            case ZMIANA_STANU -> wykonajZmianeStanu(idCelu, parametryJson, osoba);
            case WYSYLKA_PISM -> wykonajWysylkePisma(idCelu, parametryJson, osoba);
            case GENEROWANIE_DOKUMENTOW -> wykonajGenerowanieDokumentu(idCelu, parametryJson, osoba);
            case AKTUALIZACJA_DANYCH -> throw new BadRequestException(
                    "Typ akcji AKTUALIZACJA_DANYCH nie ma zdefiniowanego kontraktu parametrów - nieobsługiwany");
            case AKTUALIZACJA_ROL -> wykonajAktualizacjeRol(idCelu, parametryJson, osoba);
        }
    }

    private void wykonajZmianeStanu(String sprawaId, String parametryJson, String osoba) {
        ParametryZmianaStanu parametry = jsonMapper.readValue(parametryJson, ParametryZmianaStanu.class);
        // Konsument działa bez JWT - role zleceniodawcy nie są przekazywane, więc przejścia z
        // wymaganą rolą są odrzucane (fail-closed, pusty zbiór ról).
        sprawaService.zmienStan(sprawaId, new ZmienStanSprawyRequest(parametry.nowyStan()), osoba, Set.of());
    }

    /** Interpretacja: UruchomAkcjeZbiorowaRequest (typCelu=SPRAWA) nie podaje dokumentId/obywatelId
     * wymaganych przez KorespondencjaService.nadajPismo - dla każdej sprawy wybierany jest
     * najnowszy dokument (po dacie utworzenia) mający PODPISANĄ aktualną wersję, a obywatel brany
     * jest z samej sprawy. Brak takiego dokumentu -> błąd elementu (bez próby nadania pisma). */
    private void wykonajWysylkePisma(String sprawaId, String parametryJson, String osoba) {
        ParametryWysylkaPism parametry = jsonMapper.readValue(parametryJson, ParametryWysylkaPism.class);
        SprawaEntity sprawa = sprawaRepository.findById(sprawaId)
                .orElseThrow(() -> new NotFoundException("Sprawa o id=" + sprawaId + " nie istnieje"));

        DokumentEntity dokument = dokumentRepository.findBySprawa_Id(sprawaId).stream()
                .filter(this::maPodpisanaAktualnaWersje)
                .max(java.util.Comparator.comparing(DokumentEntity::getDataUtworzenia))
                .orElseThrow(() -> new BadRequestException(
                        "Sprawa o id=" + sprawaId + " nie ma żadnego dokumentu z podpisaną aktualną wersją - nie można nadać pisma"));

        korespondencjaService.nadajPismo(new UtworzPrzesylkeRequest(
                sprawaId, dokument.getId(), sprawa.getObywatel().getId(), parametry.kanal()), osoba);
    }

    private boolean maPodpisanaAktualnaWersje(DokumentEntity dokument) {
        return wersjaDokumentuRepository.findByDokument_IdAndWersja(dokument.getId(), dokument.getAktualnaWersja())
                .map(WersjaDokumentuEntity::isPodpisana)
                .orElse(false);
    }

    /** Interpretacja (scoping): bez jasper_service nie da się wyrenderować realnego PDF-a - tworzony
     * jest wiersz metadanych dokumentu (+ pierwsza, niepodpisana wersja) reprezentujący "dokument
     * wygenerowany z szablonu", analogicznie do tego, co realnie obsługuje ten moduł gdzie indziej
     * (DokumentEntity/WersjaDokumentuEntity to tylko metadane - treść żyje poza document_api). */
    private void wykonajGenerowanieDokumentu(String sprawaId, String parametryJson, String osoba) {
        ParametryGenerowanieDokumentow parametry = jsonMapper.readValue(parametryJson, ParametryGenerowanieDokumentow.class);
        SprawaEntity sprawa = sprawaRepository.findById(sprawaId)
                .orElseThrow(() -> new NotFoundException("Sprawa o id=" + sprawaId + " nie istnieje"));
        SzablonJasperEntity szablon = szablonJasperRepository.findById(parametry.szablonId())
                .orElseThrow(() -> new NotFoundException("Szablon Jasper o id=" + parametry.szablonId() + " nie istnieje"));

        DokumentEntity dokument = new DokumentEntity();
        dokument.setId(UUID.randomUUID().toString());
        dokument.setSprawa(sprawa);
        dokument.setTypDokumentu(szablon.getTypDokumentu());
        dokument.setNazwa(szablon.getNazwa());
        dokument.setAktualnaWersja(1);
        dokument.setDataUtworzenia(LocalDate.now());
        dokumentRepository.save(dokument);

        WersjaDokumentuEntity wersja = new WersjaDokumentuEntity();
        wersja.setId(UUID.randomUUID().toString());
        wersja.setDokument(dokument);
        wersja.setWersja(1);
        wersja.setPodpisana(false);
        wersja.setDataUtworzenia(LocalDate.now());
        wersja.setAutor(osoba);
        wersja.setStatusPodpisu(StatusPodpisu.BRAK.getWartosc());
        wersjaDokumentuRepository.save(wersja);
    }

    private void wykonajAktualizacjeRol(String uzytkownikId, String parametryJson, String osoba) {
        ParametryAktualizacjaRol parametry = jsonMapper.readValue(parametryJson, ParametryAktualizacjaRol.class);
        UzytkownikEntity uzytkownik = uzytkownikRepository.findById(uzytkownikId)
                .orElseThrow(() -> new NotFoundException("Użytkownik o id=" + uzytkownikId + " nie istnieje"));

        Set<String> role = new LinkedHashSet<>(uzytkownik.getRole());
        List<RolaUzytkownika> dodaj = parametry.dodajRole() == null ? List.of() : parametry.dodajRole();
        List<RolaUzytkownika> usun = parametry.usunRole() == null ? List.of() : parametry.usunRole();
        dodaj.forEach(rola -> role.add(AdministracjaMapper.rolaToDb(rola)));
        usun.forEach(rola -> role.remove(AdministracjaMapper.rolaToDb(rola)));

        uzytkownik.setRole(role);
        uzytkownikRepository.save(uzytkownik);

        auditLogService.zarejestrujZdarzenie(TypOperacjiAudytu.ZMIANA_DANYCH_UZYTKOWNIKA, osoba,
                "Użytkownik " + uzytkownik.getLogin(), "Zmieniono role na: " + role);
    }
}
