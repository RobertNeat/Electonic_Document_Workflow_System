# Przepływ: Wymiana EDI/XML i akcje zbiorowe

## Zakres

Moduł `features/wymiana/` w `document_web` (import komunikatu sprawy XML, import faktury kosztowej
UBL/PEPPOL, akcje zbiorowe na sprawach) oraz odpowiadające mu endpointy REST w `document_api`
(`WymianaController`, `AkcjeZbioroweController`). Analiza sprawdza, czy cały przepływ da się
wykonać z poziomu UI, bez surowych zapytań REST.

## Kroki przepływu (UI)

1. Przegląd modułu — ekran `/wymiana` (`WymianaStrona`), zakładki "Wymiana XML (EZD)" i "Wymiana
   UBL/PEPPOL" — historia wymiany. Endpoint: `GET /api/wymiana/xml/historia`,
   `GET /api/wymiana/ubl/historia`.
2. Import komunikatu XML — przycisk "Nowy import" na zakładce XML → `/wymiana/import-xml`
   (`ImportXml`), upload pliku (`p-fileUpload`, pole `plik`), wynik walidacji + podgląd mapowania na
   pola `Sprawa` wyświetlany w tabeli. Endpoint: `POST /api/wymiana/xml` (multipart). Odpowiedź 200
   z `sukces=false` to normalny wynik walidacji semantycznej (błędy pokazywane w UI), nie błąd HTTP.
3. Import faktury UBL — przycisk "Nowy import" na zakładce UBL → `/wymiana/import-ubl`
   (`ImportUbl`), upload pliku, wynik walidacji + podgląd zmapowanej faktury (kontrahent, NIP, kwota,
   pozycje, proponowany numer referencyjny) wyświetlany w tabeli. Endpoint: `POST /api/wymiana/ubl`
   (multipart).
4. Akcje zbiorowe — ekran `/wymiana/akcje-zbiorowe` (`AkcjeZbiorowe`): wybór wielu spraw z listy
   (`GET /api/sprawy`) i uruchomienie jednej z trzech akcji (zmiana stanu, wysyłka pism, generowanie
   dokumentów z szablonu — lista szablonów z `GET /api/dokumenty/szablony`). Endpoint uruchomienia:
   `POST /api/akcje-zbiorowe` (202 Accepted, request z dyskryminatorem `typAkcji`). Żądanie trafia na
   RabbitMQ i jest przetwarzane asynchronicznie przez `AkcjaZbiorowaConsumer` /
   `AkcjaZbiorowaProcessingService`.
5. Status akcji zbiorowej — po uruchomieniu UI automatycznie odpytuje
   `GET /api/akcje-zbiorowe/:akcjaId/status` co 1,2 s (polling zatrzymuje się automatycznie po
   zakończeniu, tzn. gdy status przestaje być `trwajaca`), wynik (postęp, status, błędy per element)
   renderowany na tej samej stronie.

## Dane testowe do manualnego przejścia przepływu

Repo nie zawiera formalnego schematu XSD/UBL — backend (`WymianaService`) nie waliduje względem
pliku `.xsd`, tylko strukturalnie/semantycznie przez DOM (komentarz w kodzie: "Brak formalnych
schematów XSD do zasobu"). Przykładowe pliki testowe używane w testach jednostkowych backendu są
więc najlepszym wzorcem formatu zgodnego z tym, co faktycznie parsuje serwis:
`apps/document_api/src/test/resources/wymiana/komunikat-sprawy-poprawny.xml` i
`apps/document_api/src/test/resources/wymiana/faktura-ubl-poprawna.xml`.

### Komunikat sprawy XML (import na `/wymiana/import-xml`)

Root `<KomunikatSprawy>`, wymagane dzieci: `znakSprawy`, `tytul`, `jrwa`, `wydzial`, `obywatelId`;
opcjonalnie `terminUstawowy` (ISO-8601 `rrrr-mm-dd`).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<KomunikatSprawy>
    <znakSprawy>WO.6220.14.2026</znakSprawy>
    <tytul>Decyzja o środowiskowych uwarunkowaniach dla budowy obwodnicy</tytul>
    <jrwa>6220</jrwa>
    <wydzial>Wydział Ochrony Środowiska</wydzial>
    <obywatelId>ob-42</obywatelId>
    <terminUstawowy>2026-12-01</terminUstawowy>
</KomunikatSprawy>
```

### Faktura UBL/PEPPOL (import na `/wymiana/import-ubl`)

Root `<Invoice>` w namespace `urn:oasis:names:specification:ubl:schema:xsd:Invoice-2`; wymagane:
`cbc:ID`, `cac:AccountingSupplierParty/cac:Party/cac:PartyLegalEntity`
(`cbc:RegistrationName` + `cbc:CompanyID` jako NIP, z fallbackiem na `PartyTaxScheme/CompanyID`),
`cac:LegalMonetaryTotal/cbc:PayableAmount`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
         xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
         xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">
    <cbc:ID>FK/2026/0142</cbc:ID>
    <cbc:IssueDate>2026-10-15</cbc:IssueDate>
    <cac:AccountingSupplierParty>
        <cac:Party>
            <cac:PartyLegalEntity>
                <cbc:RegistrationName>Elektromontaż Mazowsze Sp. z o.o.</cbc:RegistrationName>
                <cbc:CompanyID>5213801228</cbc:CompanyID>
            </cac:PartyLegalEntity>
            <cac:PartyTaxScheme>
                <cbc:CompanyID>5213801228</cbc:CompanyID>
                <cac:TaxScheme>
                    <cbc:ID>VAT</cbc:ID>
                </cac:TaxScheme>
            </cac:PartyTaxScheme>
        </cac:Party>
    </cac:AccountingSupplierParty>
    <cac:LegalMonetaryTotal>
        <cbc:PayableAmount currencyID="PLN">7380.50</cbc:PayableAmount>
    </cac:LegalMonetaryTotal>
    <cac:InvoiceLine>
        <cbc:InvoicedQuantity unitCode="C62">2</cbc:InvoicedQuantity>
        <cbc:LineExtensionAmount currencyID="PLN">7380.50</cbc:LineExtensionAmount>
        <cac:Item>
            <cbc:Name>Przegląd i konserwacja instalacji elektrycznej budynku urzędu</cbc:Name>
        </cac:Item>
        <cac:Price>
            <cbc:PriceAmount currencyID="PLN">3690.25</cbc:PriceAmount>
        </cac:Price>
    </cac:InvoiceLine>
</Invoice>
```

Plik niezgodny z namespace UBL lub z brakującymi polami (np.
`apps/document_api/src/test/resources/wymiana/faktura-ubl-brak-pol.xml`,
`.../faktura-ubl-niepoprawny-namespace.xml`,
`.../komunikat-sprawy-niepoprawny.xml`) przydatny do manualnego sprawdzenia ścieżki błędu — wynik
`sukces=false` z listą `BladWalidacjiXsd` jest renderowany w UI (tabela błędów), nie trzeba
sprawdzać logów backendu.

### Akcje zbiorowe

Wymaga tylko wyboru istniejących spraw w tabeli UI (dane z `GET /api/sprawy`) — nie wymaga
przygotowania plików.

## Braki / działania niemożliwe z UI

1. **Eksport komunikatów XML nie jest zaimplementowany backendowo i nie ma odpowiednika w UI.**
   Dowód:
   - Komentarz w `apps/document_web/src/app/core/api/wymiana.api.ts`: "Eksport nie jest
     zaimplementowany backendowo."
   - `apps/document_api/src/main/java/robert_neat/document_api/controller/WymianaController.java`
     udostępnia jedynie `POST /api/wymiana/xml`, `POST /api/wymiana/ubl`,
     `GET /api/wymiana/xml/historia`, `GET /api/wymiana/ubl/historia` — brak endpointu eksportu.
   - Model `KierunekWymiany` (`.../dto/wymiana/KierunekWymiany.java`) zawiera wartość `EKSPORT`, ale
     backend nigdy jej nie zapisuje (`WymianaService.zapiszHistorie(...)` zawsze ustawia
     `wpis.setKierunek("import")`) — wartość istnieje tylko jako artefakt modelu/etykiety UI
     (`WymianaStrona.kierunekLabel()` obsługuje `'eksport'`, ale nic go nigdy nie wyemituje).
   - W UI (`wymiana-strona.html`) są wyłącznie przyciski "Nowy import" na obu zakładkach — brak
     jakiegokolwiek przycisku/akcji eksportu komunikatu.
   - Konsekwencja: przepływ "elektroniczna wymiana XML z podmiotami zewnętrznymi" wspomniany w
     `docs/development_docs/main_assumptions.md` (sekcja o imporcie/eksporcie) jest zrealizowany
     tylko w kierunku przychodzącym (import); nadawanie komunikatów nie jest możliwe z żadnego
     miejsca w aplikacji (ani UI, ani REST — bo endpointu nie ma).

2. **Walidacja XSD w ścisłym sensie nie istnieje** — repo nie zawiera plików `.xsd`; walidacja jest
   realizowana ręcznie przez parsowanie DOM i sprawdzanie obecności/formatu wybranych elementów
   (potwierdzone komentarzem w `WymianaService`: "Brak formalnych schematów XSD do zasobu"). Nie jest
   to luka UI (błędy walidacji są poprawnie wyświetlane w UI), ale odbiega od opisu w
   `main_assumptions.md`, który mówi o "zdefiniowanym schemacie XSD" — w praktyce schemat jest
   niejawny/zakodowany w logice serwisu, a nie plikiem XSD możliwym do zacytowania.

3. **Akcje zbiorowe — poza tymi trzema UI nie obsługuje żadnych innych typów.** `AkcjeZbiorowe`
   (frontend) ogranicza `TypAkcjiUi` do `ZMIANA_STANU | WYSYLKA_PISM | GENEROWANIE_DOKUMENTOW`, mimo
   że `TypAkcjiZbiorowej` (wspólny DTO) może zawierać więcej wartości — jeśli backend obsługuje
   dodatkowe typy akcji zbiorowych, nie mają one UI. (Nie potwierdzono dodatkowych typów w
   `AkcjaZbiorowaProcessingService` ponad te trzy — do weryfikacji przy ewentualnym rozszerzeniu
   enuma `TypAkcjiZbiorowej`.)

Poza punktem 1 (brak eksportu) przepływ importu XML/UBL i akcji zbiorowych jest w pełni dostępny i
obsługiwany z UI, łącznie z wyświetlaniem błędów walidacji i statusu asynchronicznego przetwarzania
(bez konieczności zaglądania w logi backendu czy bazę danych).
