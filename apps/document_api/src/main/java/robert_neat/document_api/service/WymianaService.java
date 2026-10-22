package robert_neat.document_api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import robert_neat.document_api.dto.wymiana.BladWalidacjiXsd;
import robert_neat.document_api.dto.wymiana.FakturaUbl;
import robert_neat.document_api.dto.wymiana.PozycjaFakturyUbl;
import robert_neat.document_api.dto.wymiana.WpisHistoriiWymiany;
import robert_neat.document_api.dto.wymiana.WynikWalidacjiUbl;
import robert_neat.document_api.dto.wymiana.WynikWalidacjiXml;
import robert_neat.document_api.entity.WpisHistoriiWymianyEntity;
import robert_neat.document_api.exception.BadRequestException;
import robert_neat.document_api.mapper.WymianaMapper;
import robert_neat.document_api.repository.WpisHistoriiWymianyRepository;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Import/walidacja komunikatów sprawy w formacie XML oraz faktur UBL 2.1 (PEPPOL BIS Billing 3.0),
 * plus wspólna historia wymiany (tabela wpis_historii_wymiany, dyskryminator typWymiany).
 * Brak formalnych schematów XSD do zasobu - walidacja strukturalna/semantyczna przez DOM, analogicznie
 * do parsowania CAMT.053 w PlatnosciService (bez biblioteki do konkretnego formatu bankowego/PEPPOL). */
@Service
@Transactional(readOnly = true)
public class WymianaService {

    /** Minimalny, własny format "komunikatu sprawy" (brak realnego schematu rządowego do zasobu) -
     * root <KomunikatSprawy> z dzieckami odwzorowującymi pola UtworzSpraweRequest/Sprawa. znakSprawy
     * i obywatelId są traktowane jako wymagane (identyfikacja sprawy/strony), tytul/jrwa/wydzial jako
     * wymagane pola biznesowe, terminUstawowy jako opcjonalny (nie każda sprawa ma termin ustawowy). */
    private static final String ROOT_KOMUNIKAT_SPRAWY = "KomunikatSprawy";

    private static final String UBL_NAMESPACE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";

    private final WpisHistoriiWymianyRepository historiaRepository;

    public WymianaService(WpisHistoriiWymianyRepository historiaRepository) {
        this.historiaRepository = historiaRepository;
    }

    // --- historia ---

    public List<WpisHistoriiWymiany> listaHistoriiXml() {
        return historiaRepository.findByTypWymianyOrderByDataDesc("xml").stream()
                .map(WymianaMapper::toDto)
                .toList();
    }

    public List<WpisHistoriiWymiany> listaHistoriiUbl() {
        return historiaRepository.findByTypWymianyOrderByDataDesc("ubl").stream()
                .map(WymianaMapper::toDto)
                .toList();
    }

    // --- import XML (komunikat sprawy) ---

    @Transactional
    public WynikWalidacjiXml importujXml(String nazwaPliku, InputStream xml) {
        List<BladWalidacjiXsd> bledy = new ArrayList<>();
        Map<String, Object> sprawaZmapowana = new LinkedHashMap<>();

        Document document;
        try {
            document = parseSecurely(xml);
        } catch (BadRequestException e) {
            bledy.add(new BladWalidacjiXsd(ROOT_KOMUNIKAT_SPRAWY, "Plik nie jest prawidłowym dokumentem XML: " + e.getMessage()));
            zapiszHistorieXml(nazwaPliku, false, 0, "Błąd parsowania XML: " + e.getMessage());
            return new WynikWalidacjiXml(false, bledy, sprawaZmapowana);
        }

        Element root = document.getDocumentElement();
        if (root == null || !ROOT_KOMUNIKAT_SPRAWY.equals(root.getLocalName() == null ? root.getTagName() : root.getLocalName())) {
            bledy.add(new BladWalidacjiXsd(ROOT_KOMUNIKAT_SPRAWY,
                    "Oczekiwano elementu głównego <" + ROOT_KOMUNIKAT_SPRAWY + ">"));
            zapiszHistorieXml(nazwaPliku, false, 0, "Niezgodność ze schematem - nieprawidłowy element główny");
            return new WynikWalidacjiXml(false, bledy, sprawaZmapowana);
        }

        wymaganyTekst(root, "znakSprawy", bledy, sprawaZmapowana);
        wymaganyTekst(root, "tytul", bledy, sprawaZmapowana);
        wymaganyTekst(root, "jrwa", bledy, sprawaZmapowana);
        wymaganyTekst(root, "wydzial", bledy, sprawaZmapowana);
        wymaganyTekst(root, "obywatelId", bledy, sprawaZmapowana);

        String terminUstawowy = tekstDziecka(root, "terminUstawowy");
        if (terminUstawowy != null && !terminUstawowy.isBlank()) {
            try {
                sprawaZmapowana.put("terminUstawowy", LocalDate.parse(terminUstawowy.trim()).toString());
            } catch (DateTimeParseException e) {
                bledy.add(new BladWalidacjiXsd("terminUstawowy", "Nieprawidłowy format daty (wymagany ISO-8601 rrrr-mm-dd): " + terminUstawowy));
            }
        }

        boolean sukces = bledy.isEmpty();
        String opis = sukces
                ? "Zaimportowano komunikat sprawy" + (sprawaZmapowana.get("tytul") != null ? ": " + sprawaZmapowana.get("tytul") : "")
                : "Niezgodność ze schematem - " + bledy.size() + " błąd(ów) walidacji";
        zapiszHistorieXml(nazwaPliku, sukces, sukces ? 1 : 0, opis);

        return new WynikWalidacjiXml(sukces, bledy, sukces ? sprawaZmapowana : Map.of());
    }

    private void wymaganyTekst(Element root, String tag, List<BladWalidacjiXsd> bledy, Map<String, Object> sprawaZmapowana) {
        String wartosc = tekstDziecka(root, tag);
        if (wartosc == null || wartosc.isBlank()) {
            bledy.add(new BladWalidacjiXsd(tag, "Brak wymaganego elementu <" + tag + ">"));
        } else {
            sprawaZmapowana.put(tag, wartosc.trim());
        }
    }

    private void zapiszHistorieXml(String nazwaPliku, boolean sukces, int liczbaRekordow, String opis) {
        zapiszHistorie("xml", nazwaPliku, sukces, liczbaRekordow, opis);
    }

    // --- import UBL (faktura PEPPOL BIS Billing 3.0) ---

    @Transactional
    public WynikWalidacjiUbl importujUbl(String nazwaPliku, InputStream xml) {
        List<BladWalidacjiXsd> bledy = new ArrayList<>();

        Document document;
        try {
            document = parseSecurely(xml);
        } catch (BadRequestException e) {
            bledy.add(new BladWalidacjiXsd("Invoice", "Plik nie jest prawidłowym dokumentem XML: " + e.getMessage()));
            zapiszHistorieUbl(nazwaPliku, false, 0, "Błąd parsowania XML: " + e.getMessage());
            return new WynikWalidacjiUbl(false, bledy, null);
        }

        Element root = document.getDocumentElement();
        String rootNs = root == null ? null : root.getNamespaceURI();
        if (root == null || !"Invoice".equals(localName(root)) || !UBL_NAMESPACE.equals(rootNs)) {
            bledy.add(new BladWalidacjiXsd("Invoice",
                    "Nieprawidłowa przestrzeń nazw UBL — oczekiwano " + UBL_NAMESPACE));
            zapiszHistorieUbl(nazwaPliku, false, 0, "Nieprawidłowa przestrzeń nazw UBL — oczekiwano " + UBL_NAMESPACE);
            return new WynikWalidacjiUbl(false, bledy, null);
        }

        String numer = firstTextByLocalName(root, "ID");
        if (numer == null || numer.isBlank()) {
            bledy.add(new BladWalidacjiXsd("cbc:ID", "Brak numeru faktury (cbc:ID)"));
        }

        String kontrahent = kontrahentNazwa(root);
        if (kontrahent == null || kontrahent.isBlank()) {
            bledy.add(new BladWalidacjiXsd("cac:AccountingSupplierParty", "Brak nazwy kontrahenta (sprzedawcy)"));
        }

        String nip = nipSprzedawcy(root);
        if (nip == null || nip.isBlank()) {
            bledy.add(new BladWalidacjiXsd("cac:PartyTaxScheme/cbc:CompanyID", "Brak numeru NIP kontrahenta"));
        }

        BigDecimal kwota = kwotaCalkowita(root);
        if (kwota == null) {
            bledy.add(new BladWalidacjiXsd("cbc:PayableAmount", "Brak lub nieprawidłowa wartość kwoty do zapłaty"));
        }

        List<PozycjaFakturyUbl> pozycje = pozycjeFaktury(root);

        if (!bledy.isEmpty()) {
            zapiszHistorieUbl(nazwaPliku, false, 0, "Niezgodność ze schematem UBL - " + bledy.size() + " błąd(ów) walidacji");
            return new WynikWalidacjiUbl(false, bledy, null);
        }

        String numerReferencyjnyProponowany = proponowanyNumerReferencyjny(numer);
        FakturaUbl faktura = new FakturaUbl(numer.trim(), kontrahent.trim(), nip.trim(), kwota, pozycje, numerReferencyjnyProponowany);

        zapiszHistorieUbl(nazwaPliku, true, 1, "Zmapowano fakturę kosztową " + numer.trim() + " od " + kontrahent.trim());
        return new WynikWalidacjiUbl(true, bledy, faktura);
    }

    /** Numer referencyjny proponowanej należności (podgląd, bez zapisu) - prefiks "FKI" (Faktura
     * Kosztowa Import) + numer faktury z usuniętymi znakami niebędącymi cyfrą/literą, by uniknąć
     * kolizji formatu z numerami generowanymi ręcznie przez księgowość (np. "FK/2026/0088"). */
    private String proponowanyNumerReferencyjny(String numerFaktury) {
        String oczyszczony = numerFaktury == null ? "" : numerFaktury.trim().replaceAll("[^A-Za-z0-9]", "");
        return "FKI/" + oczyszczony;
    }

    private String kontrahentNazwa(Element root) {
        Element supplierParty = firstElementByLocalName(root, "AccountingSupplierParty");
        if (supplierParty == null) {
            return null;
        }
        Element party = firstElementByLocalName(supplierParty, "Party");
        if (party == null) {
            return null;
        }
        Element legalEntity = firstElementByLocalName(party, "PartyLegalEntity");
        if (legalEntity != null) {
            String nazwa = firstTextByLocalName(legalEntity, "RegistrationName");
            if (nazwa != null && !nazwa.isBlank()) {
                return nazwa;
            }
        }
        Element partyName = firstElementByLocalName(party, "PartyName");
        if (partyName != null) {
            String nazwa = firstTextByLocalName(partyName, "Name");
            if (nazwa != null && !nazwa.isBlank()) {
                return nazwa;
            }
        }
        return null;
    }

    /** NIP szukany w dwóch możliwych lokalizacjach PEPPOL BIS: PartyLegalEntity/CompanyID (częstsze)
     * i PartyTaxScheme/CompanyID (fallback) - różni dostawcy oprogramowania księgowego umieszczają
     * identyfikator podatkowy w różnych miejscach struktury UBL. */
    private String nipSprzedawcy(Element root) {
        Element supplierParty = firstElementByLocalName(root, "AccountingSupplierParty");
        if (supplierParty == null) {
            return null;
        }
        Element party = firstElementByLocalName(supplierParty, "Party");
        if (party == null) {
            return null;
        }
        Element legalEntity = firstElementByLocalName(party, "PartyLegalEntity");
        if (legalEntity != null) {
            String companyId = firstTextByLocalName(legalEntity, "CompanyID");
            if (companyId != null && !companyId.isBlank()) {
                return companyId;
            }
        }
        Element taxScheme = firstElementByLocalName(party, "PartyTaxScheme");
        if (taxScheme != null) {
            String companyId = firstTextByLocalName(taxScheme, "CompanyID");
            if (companyId != null && !companyId.isBlank()) {
                return companyId;
            }
        }
        return null;
    }

    /** Kwota całkowita: LegalMonetaryTotal/PayableAmount (ścieżka kanoniczna PEPPOL BIS), z fallbackiem
     * na bezpośredni PayableAmount pod rootem gdyby producent spłaszczył strukturę. */
    private BigDecimal kwotaCalkowita(Element root) {
        Element total = firstElementByLocalName(root, "LegalMonetaryTotal");
        String tekst = total != null ? firstTextByLocalName(total, "PayableAmount") : firstTextByLocalName(root, "PayableAmount");
        if (tekst == null || tekst.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(tekst.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<PozycjaFakturyUbl> pozycjeFaktury(Element root) {
        List<PozycjaFakturyUbl> pozycje = new ArrayList<>();
        NodeList linie = elementsByLocalName(root, "InvoiceLine");
        for (int i = 0; i < linie.getLength(); i++) {
            Element linia = (Element) linie.item(i);
            String nazwa = null;
            Element item = firstElementByLocalName(linia, "Item");
            if (item != null) {
                nazwa = firstTextByLocalName(item, "Name");
            }
            BigDecimal ilosc = parseBigDecimal(firstTextByLocalName(linia, "InvoicedQuantity"));
            Element price = firstElementByLocalName(linia, "Price");
            BigDecimal cenaJednostkowa = price != null ? parseBigDecimal(firstTextByLocalName(price, "PriceAmount")) : null;
            BigDecimal wartosc = parseBigDecimal(firstTextByLocalName(linia, "LineExtensionAmount"));
            pozycje.add(new PozycjaFakturyUbl(nazwa, ilosc, cenaJednostkowa, wartosc));
        }
        return pozycje;
    }

    private BigDecimal parseBigDecimal(String tekst) {
        if (tekst == null || tekst.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(tekst.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void zapiszHistorieUbl(String nazwaPliku, boolean sukces, int liczbaRekordow, String opis) {
        zapiszHistorie("ubl", nazwaPliku, sukces, liczbaRekordow, opis);
    }

    // --- wspolne ---

    private void zapiszHistorie(String typWymiany, String nazwaPliku, boolean sukces, int liczbaRekordow, String opis) {
        WpisHistoriiWymianyEntity wpis = new WpisHistoriiWymianyEntity();
        wpis.setId(UUID.randomUUID().toString());
        wpis.setTypWymiany(typWymiany);
        wpis.setNazwaPliku(nazwaPliku);
        wpis.setKierunek("import");
        wpis.setData(OffsetDateTime.now());
        wpis.setStatus(sukces ? "sukces" : "blad_walidacji");
        wpis.setLiczbaRekordow(liczbaRekordow);
        // opis zapisywany do varchar(1024) - obcięcie, by dane wejściowe uzytkownika (np. długi tytuł
        // sprawy lub nazwa kontrahenta wplecione w komunikat) nie spowodowały błędu zapisu do bazy.
        wpis.setOpis(opis != null && opis.length() > 1024 ? opis.substring(0, 1024) : opis);
        historiaRepository.save(wpis);
    }

    /** XXE: DOCTYPE i zewnętrzne encje wyłączone, bo wejście jest nieuwierzytelnionym plikiem użytkownika -
     * identyczny wzorzec jak PlatnosciService.parseSecurely dla CAMT.053. */
    private Document parseSecurely(InputStream xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(xml);
        } catch (Exception e) {
            throw new BadRequestException("Nie udało się przetworzyć pliku XML: " + e.getMessage());
        }
    }

    private String localName(Element element) {
        return element.getLocalName() == null ? element.getTagName() : element.getLocalName();
    }

    /** Pierwsze dziecko o danej nazwie lokalnej bezpośrednio pod elementem (nie getElementsByTagName,
     * żeby nie złapać zagnieżdżonego elementu o tej samej nazwie z głębszego poziomu) - namespace-agnostic
     * przez porównanie localName, bo prefiksy cbc:/cac: nie są gwarantowane identyczne u każdego producenta. */
    private Element firstElementByLocalName(Element rodzic, String nazwaLokalna) {
        NodeList dzieci = rodzic.getChildNodes();
        for (int i = 0; i < dzieci.getLength(); i++) {
            Node dziecko = dzieci.item(i);
            if (dziecko instanceof Element el && nazwaLokalna.equals(localName(el))) {
                return el;
            }
        }
        return null;
    }

    private String firstTextByLocalName(Element rodzic, String nazwaLokalna) {
        Element dziecko = firstElementByLocalName(rodzic, nazwaLokalna);
        return dziecko == null ? null : dziecko.getTextContent();
    }

    /** Wersja getElementsByTagNameNS/lokalna-nazwa dla powtarzalnych elementów (np. InvoiceLine) -
     * przeszukuje całe poddrzewo, w przeciwieństwie do firstElementByLocalName (tylko dzieci bezpośrednie). */
    private NodeList elementsByLocalName(Element root, String nazwaLokalna) {
        return root.getElementsByTagNameNS("*", nazwaLokalna);
    }

    private String tekstDziecka(Element rodzic, String tag) {
        NodeList dzieci = rodzic.getChildNodes();
        for (int i = 0; i < dzieci.getLength(); i++) {
            if (dzieci.item(i) instanceof Element el && tag.equals(localName(el))) {
                return el.getTextContent();
            }
        }
        return null;
    }
}
