# Przepływ: Wymiana EDI/XML i akcje zbiorowe

## Zakres

Scenariusz testowy do manualnego przejścia: import komunikatu sprawy XML, import faktury kosztowej
UBL, oraz akcje zbiorowe na sprawach (zmiana stanu, wysyłka pism, generowanie dokumentów).
Aplikacja: `document_web` (`http://localhost:10600`).

## Aktorzy i role

| Login w Keycloaku | Rola       | Kiedy potrzebny                                 |
| ----------------- | ---------- | ----------------------------------------------- |
| `demo.referent`   | referent   | import XML, akcje zbiorowe, import UBL          |
| `demo.ksiegowosc` | ksiegowosc | alternatywnie do importu UBL (faktury kosztowe) |

Hasło obu kont: wspólne dla kont demo, zmienna `KEYCLOAK_DEMO_USER_PASSWORD` (domyślnie
`change-me`).

## Kroki przepływu

1. Zaloguj się jako `demo.referent`. Przejdź do menu **Wymiana EDI / XML**.
2. Zobaczysz trzy zakładki: **"Wymiana XML (EZD)"**, **"Wymiana UBL/PEPPOL"** i **"Akcje
   zbiorowe"**. Pierwsze dwie mają historię importów.
3. **Import komunikatu XML** — na zakładce "Wymiana XML (EZD)" kliknij **"Nowy import"**, wybierz
   plik `.xml` [file, required]. Element główny musi mieć nazwę `<KomunikatSprawy>` — inna nazwa
   elementu głównego powoduje błąd walidacji. Pola wewnątrz:
   - `znakSprawy` [text, required — błąd walidacji jeśli brak/puste]
   - `tytul` [text, required]
   - `jrwa` [text, required — **nie jest sprawdzane, że symbol istnieje w słowniku JRWA**, akceptowany dowolny ciąg]
   - `wydzial` [text, required — **nie jest sprawdzane przeciw liście 4 wydziałów**, akceptowany dowolny ciąg]
   - `obywatelId` [text, required — **nie jest sprawdzane istnienie w bazie na etapie walidacji XML**]
   - `terminUstawowy` [date, optional — jeśli obecne, musi być w formacie ISO-8601 `rrrr-mm-dd`, inaczej błąd "Nieprawidłowy format daty"]

   Przykład poprawnego pliku:

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

   Gotowe pliki testowe w repozytorium: `apps/document_api/src/test/resources/wymiana/
komunikat-sprawy-poprawny.xml` (poprawny) oraz `komunikat-sprawy-niepoprawny.xml` (do testu
   ścieżki błędu). Prześlij plik — wynik walidacji i podgląd zmapowanych pól pojawi się na tej
   samej stronie; komunikat "sukces=false" to normalny wynik walidacji, nie błąd aplikacji.
4. **Import faktury UBL** — na zakładce "Wymiana UBL/PEPPOL" kliknij **"Nowy import"**, wybierz
   plik `.xml` [file, required]. Walidacja (DOM, brak formalnego XSD):
   - Namespace roota musi być `urn:oasis:names:specification:ubl:schema:xsd:Invoice-2`, element `Invoice` — inny namespace → błąd "Nieprawidłowa przestrzeń nazw UBL".
   - `cbc:ID` (numer faktury) [text, required]
   - Nazwa kontrahenta (`PartyLegalEntity/RegistrationName`, fallback `PartyName/Name`) [text, required]
   - NIP kontrahenta (`PartyLegalEntity/CompanyID`, fallback `PartyTaxScheme/CompanyID`) [text, required — **brak walidacji formatu/sumy kontrolnej NIP**, tylko sprawdzenie obecności]
   - Kwota do zapłaty (`LegalMonetaryTotal/PayableAmount`) [liczba, required — musi parsować się jako wartość dziesiętna, inaczej błąd "Brak lub nieprawidłowa wartość kwoty"]

   Gotowy plik testowy: `apps/document_api/src/test/resources/wymiana/faktura-ubl-poprawna.xml`
   (numer faktury `FK/2026/0099`, kontrahent "Budimex Serwis Sp. z o.o.", NIP `5260255999`, kwota
   4500,00 PLN). Pliki do testu ścieżki błędu: `faktura-ubl-brak-pol.xml` (brakujące pola) i
   `faktura-ubl-niepoprawny-namespace.xml` (błędny namespace). Prześlij plik — zobaczysz podgląd
   zmapowanej faktury: kontrahent, NIP, kwota, pozycje (oba błędne pliki zwrócą listę błędów
   walidacji widoczną w interfejsie). Import jest tylko podglądem (mapowanie i proponowany numer
   referencyjny) — nie zapisuje żadnej należności w bazie (zob. `WymianaService.importujUbl`), więc
   nie koliduje z należnością demo "Usługi serwisowe — faktura" (również 4500 zł, zob. "Rozliczanie
   płatności") mimo identycznej kwoty.
5. **Akcje zbiorowe** — kliknij zakładkę **"Akcje zbiorowe"** w module Wymiana EDI / XML.
6. Na zakładce "Akcje zbiorowe" są trzy karty, jedna pod drugą:
   - **"Masowa zmiana stanu spraw"** — zaznacz checkboxem jedną lub więcej spraw [checkbox,
     min. 1 wymagana], np. `WO.6220.14.2026`, `WO.6220.21.2026`, `WO.6220.23.2026` (wszystkie
     trzy w wydziale Ochrony Środowiska, stan "W toku"), wybierz nowy stan z listy [select,
     required — lista stanów jest zahardkodowana we froncie jako suma stanów z różnych obiegów:
     `"Zarejestrowana"`, `"W toku"`, `"Do akceptacji"`, `"Zaakceptowana"`, `"Zakończona"`,
     `"Zamknięta"`; **nic nie blokuje wybrania stanu niewłaściwego dla obiegu danej sprawy** na
     tym ekranie], np. `"Do akceptacji"`, i kliknij **"Uruchom zmianę stanu"**.
   - **"Masowa wysyłka pism"** — zaznacz sprawy, wybierz kanał wysyłki [select, required —
     `"e-Doręczenia"` / `"Operator pocztowy"` / `"E-mail"`; **ekran nie sprawdza z wyprzedzeniem
     adresu ADE ani statusu podpisu dokumentu** — błędy per-sprawa pojawią się na liście wyników
     po uruchomieniu], kliknij **"Uruchom wysyłkę"**.
   - **"Masowe generowanie dokumentów z szablonu"** — zaznacz sprawy, wybierz szablon [select,
     required — lista pokazuje nazwy plików, nie typy dokumentów], np. `"decyzja_srodowiskowa.jrxml"`
     (jedyny szablon typu "Decyzja administracyjna", zob. dokument "Sprawy i obiegi", krok 7),
     kliknij **"Uruchom generowanie"**.
7. Po uruchomieniu każdej z akcji pod przyciskiem pojawi się pasek postępu, aktualizowany
   automatycznie (co ok. 1-2 s) do zakończenia operacji. Wyniki błędne dla poszczególnych spraw są
   wypisane poniżej paska.

## Znane ograniczenia (czego nie da się zrobić w aplikacji)

1. **Lista Obywateli ma swój osobny przycisk "Akcja zbiorowa"** — jest on jednak trwale wyszarzony
   i niezwiązany z zakładką "Akcje zbiorowe" w module Wymiana EDI / XML.
2. **Nie da się wysłać (eksportować) komunikatu XML do systemu zewnętrznego** — tylko odbieranie
   (import) jest zaimplementowane. Brak przycisku eksportu w całym module Wymiana.
3. **Akcje zbiorowe obsługują wyłącznie te trzy typy operacji** widoczne na ekranie — żadnych
   innych.
