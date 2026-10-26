# Przepływ: Dokumenty, szablony i podpis elektroniczny

## Zakres

Weryfikacja, czy przepływ: wgranie szablonu Jasper → wygenerowanie dokumentu z szablonu i danych
sprawy → podgląd PDF → podpisanie dokumentu certyfikatem (PAdES) → sprawdzenie szczegółów/statusu
podpisu da się przejść w całości z poziomu UI `document_web`, bez surowych zapytań REST. Obejmuje
moduł `features/dokumenty` (lista, szczegóły, szablony), moduł `features/wymiana/akcje-zbiorowe`
(generowanie dokumentu z szablonu — ta akcja nie ma odpowiednika w module Dokumenty), oraz backendy
`document_api`, `dms_service`, `jasper_service`, `sign_service`.

## Kroki przepływu (UI)

1. **Wgranie szablonu Jasper** — ekran `Dokumenty → Szablony` (`szablony-strona`), przycisk
   "Wgraj szablon" (dialog z wyborem typu dokumentu + `p-fileUpload` dla plików `.jrxml`) —
   endpoint: `POST /api/szablony` (`document_api`, proxy do `dms_service POST /api/szablony`).
2. **Przegląd listy i historii wersji szablonu** — tabela szablonów, kliknięcie wiersza (nie
   dedykowany przycisk) otwiera dialog "Historia wersji szablonu" — endpoint: `GET /api/szablony`
   i `GET /api/szablony/{id}/historia`.
3. **Wygenerowanie dokumentu z szablonu i danych sprawy** — ekran `Wymiana → Akcje zbiorowe`
   (`akcje-zbiorowe`), karta "Generowanie dokumentów": wybór jednej lub wielu spraw z tabeli +
   wybór szablonu z listy rozwijanej, przycisk uruchomienia — endpoint:
   `POST /api/akcje-zbiorowe` (`typAkcji=GENEROWANIE_DOKUMENTOW`, 202, przetwarzanie
   asynchroniczne przez RabbitMQ), status śledzony pollingiem
   `GET /api/akcje-zbiorowe/{akcjaId}/status` co 1,2 s. Serwer: renderuje PDF przez
   `jasper_service` (`POST /api/render/{szablonId}` z danymi sprawy/obywatela zbudowanymi
   przez `budujParametryRaportu`), zapisuje wynik jako nową wersję w `dms_service`
   (`POST /api/dokumenty/{dokumentId}/wersje`) i tworzy wiersz `core.dokument`.
   **Nie istnieje odpowiednik tej akcji w samym module Dokumenty** — nie można z karty
   szczegółów sprawy czy z listy dokumentów wybrać "wygeneruj nowy dokument z szablonu";
   jedyna droga UI to akcje zbiorowe (działa też dla jednej, zaznaczonej pojedynczo sprawy).
4. **Lista dokumentów sprawy** — ekran `Dokumenty` (`dokumenty-strona`), filtrowanie po
   sprawie/typie dokumentu — endpoint: `GET /api/dokumenty?sprawaId=...&typDokumentuId=...`.
5. **Szczegóły dokumentu i osi czasu wersji** — ekran `Dokumenty/{id}` (`dokument-szczegoly`) —
   endpointy: `GET /api/dokumenty/{id}`, `GET /api/dokumenty/{id}/wersje`.
6. **Podgląd PDF wersji w dialogu (bez pobierania pliku)** — przycisk "Podgląd" na wersji w osi
   czasu, iframe z blob URL — endpoint: `GET /api/dokumenty/{id}/wersje/{wersja}/plik`.
7. **Pobranie pliku wersji (roboczej lub podpisanej)** — przycisk "Pobierz" — ten sam endpoint,
   front wymusza `<a download>`.
8. **Podpisanie dokumentu certyfikatem (PAdES)** — przycisk "Podpisz" widoczny tylko na aktualnej
   (niepodpisanej) wersji, otwiera cert-picker — lista certyfikatów z
   `GET /api/certyfikaty` (proxy do `sign_service`), wybór certyfikatu (radio button, tylko
   aktywne i niewygasłe są zaznaczalne), zatwierdzenie — endpoint:
   `POST /api/dokumenty/{id}/podpisz` (orkiestracja: pobranie bajtów z `dms_service`,
   `sign_service POST /api/podpisy/pades` ze znakowaniem czasem TSA automatycznym — brak
   osobnej akcji TSA, jest zawsze częścią podpisania BASELINE-T, zapis nowej podpisanej wersji
   w `dms_service` + lokalnie).
9. **Podgląd szczegółów podpisu** — przycisk "Szczegóły podpisu" na podpisanej wersji, dialog z
   podpisującym (DN certyfikatu), czasem podpisu, odciskiem certyfikatu (SHA-256), statusem
   OCSP/CRL i znacznikiem czasu TSA — endpoint: `GET /api/dokumenty/{id}/wersje/{wersja}/podpis`
   (realna weryfikacja kryptograficzna w `sign_service POST /api/podpisy/weryfikacja`, nie odczyt
   wartości zapamiętanej przy podpisaniu).

## Dane testowe do manualnego przejścia przepływu

- **Typ dokumentu**: "Decyzja administracyjna" (`td-3`, seed `document_api`).
- **Szablon Jasper do wgrania**: plik `decyzja_administracyjna_wodna.jrxml` (format
  JasperReports 7.x — bez `xmlns` na `<jasperReport>`, bandy bez `<band>`, `<element kind="...">`
  zamiast `<reportElement>` — wzór: `apps/jasper_service/src/test/resources/testowy-szablon.jrxml`),
  treść: pismo decyzji środowiskowej z polami parametrów raportu faktycznie budowanymi przez
  `AkcjaZbiorowaProcessingService.budujParametryRaportu` — `$P{znakSprawy}`, `$P{tytulSprawy}`,
  `$P{wydzial}`, `$P{referent}`, `$P{dataWszczecia}`, `$P{terminUstawowy}`, `$P{obywatelImie}`,
  `$P{obywatelNazwisko}`, `$P{obywatelNazwaPodmiotu}`, `$P{obywatelPesel}`, `$P{obywatelNip}`,
  `$P{obywatelEmail}`, `$P{obywatelTelefon}` (dowolny inny parametr w szablonie renderuje się jako
  puste pole — jasper_service nie dostaje żadnych innych danych). Przykładowy tekst statyczny
  pisma: "Na podstawie art. 104 Kodeksu postępowania administracyjnego oraz art. 71 ustawy o
  udostępnianiu informacji o środowisku, po rozpatrzeniu wniosku strony $P{obywatelNazwaPodmiotu},
  zatwierdza się warunki środowiskowe realizacji przedsięwzięcia w ramach sprawy $P{znakSprawy}."
- **Sprawa do wygenerowania dokumentu**: znak sprawy `WO.6220.14.2026` (seed, Wydział Ochrony
  Środowiska), strona: Budimex Serwis Sp. z o.o.
- **Osoba podpisująca (certyfikat demo aktywny)**: `cert-1`, `CN=Katarzyna Zielińska, O=Urząd,
  C=PL`, nr seryjny `7A3F1B2C9D0E4455`, ważny do 2027-01-10 — jedyny certyfikat w seedzie, który
  faktycznie przejdzie walidację `status='aktywny' AND data_wygasniecia > dziś` przy podpisywaniu.
- **Certyfikaty do testowania ścieżek błędu** (widoczne w tym samym cert-pickerze, ale
  wizualnie/logicznie wyłączone z wyboru w UI): `cert-2` `CN=Piotr Lewandowski` (status
  `wygasly`), `cert-3` `CN=Marta Wiśniewska` (status `uniewazniony`, z wpisem w rejestrze
  unieważnień `un-1`, powód `key_compromise`).

## Pokrycie endpointów (dms_service, jasper_service, sign_service, document_api/dokumenty)

| Endpoint | Ma odpowiednik w UI? | Uwaga |
| --- | --- | --- |
| `dms_service POST /api/szablony` | Tak (proxy `document_api`) | Krok 1 |
| `dms_service GET /api/szablony`, `/{id}` | Tak (proxy przez `document_api core` po replikacji) | — |
| `dms_service GET /api/szablony/{id}/plik` | **Nie** | Brak proxy w `document_api`; brak podglądu treści `.jrxml` z UI |
| `dms_service POST /api/dokumenty/{id}/wersje`, `GET .../wersje`, `GET .../wersje/{w}`, `GET .../wersje/{w}/plik` | Tak, wywoływane wewnętrznie przez `document_api` (generowanie, podpisanie, podgląd/pobranie) | Użytkownik nie woła ich bezpośrednio — zawsze przez orkiestrację `document_api` |
| `jasper_service POST /api/render/{szablonId}` | Tak, wywoływany wewnętrznie z akcji zbiorowej "Generowanie dokumentów" i z generowania wezwania | Brak bezpośredniego UI do tego endpointu (poprawnie — to wewnętrzny serwis renderujący, bez własnego UI z założenia) |
| `sign_service POST /api/podpisy/pades` | Tak (przez `document_api POST /api/dokumenty/{id}/podpisz`) | Krok 8 |
| `sign_service POST /api/podpisy/weryfikacja` | Tak (przez `document_api GET /api/dokumenty/{id}/wersje/{w}/podpis`) | Krok 9 |
| `sign_service POST /api/podpisy/xades`, `POST /api/podpisy/weryfikacja-xades` | **Nie** | `document_api` nie woła tych endpointów wcale (potwierdzone w `PodpisController.java` i kontrakcie `sign_service.md`) |
| `sign_service GET /api/certyfikaty`, `/{id}` | Tak (proxy `CertyfikatController` w `document_api`, cert-picker) | Tylko odczyt — zob. brak 1 poniżej |
| `sign_service POST /api/ocsp`, `GET /api/crl/issuing-ca.crl` | Nie i nie powinny | Protokoły maszyna-maszyna (RFC 6960/CRL), odpytywane wyłącznie przez `WeryfikacjaService` w `sign_service`, nigdy przez użytkownika z przeglądarki |
| `document_api POST /api/naleznosci/{id}/wezwanie` | **Nie — frontend to mock** | `platnosci-strona.ts`, `potwierdzWezwanie()`: komentarz w kodzie "Mock — rzeczywiste wygenerowanie pisma z szablonu realizuje moduł Dokumenty"; endpoint realnie istnieje i działa w `document_api`/`PlatnosciController`, ale front nie woła go |

## Braki / działania niemożliwe z UI

1. **Rejestracja/generowanie certyfikatu i unieważnienie certyfikatu nie są dostępne z UI w
   żadnej formie — brak endpointu, nie tylko brak ekranu.** Cały materiał PKI (root CA, issuing
   CA, TSA, certyfikaty leaf `cert-1`/`cert-2`/`cert-3`) jest tworzony programowo przy starcie
   `sign_service` i/lub wstrzykiwany przez Liquibase seed
   (`apps/sign_service/src/main/resources/db/changelog/demo/100-seed.xml`,
   `.../pki/00*-*.xml`). `sign_service.PodpisController`/`CertyfikatController` wystawiają
   wyłącznie `GET /api/certyfikaty(/{id})` — nie istnieje `POST`/`PUT`/`DELETE` dla certyfikatów
   ani endpoint unieważnienia (tabela `pki.uniewaznienie` jest zasilana tylko przez seed
   `100-03-uniewaznienia`, `un-1` dla `cert-3`). Skutek: certyfikat podpisujący **nigdy nie
   przejdzie z `aktywny` na `uniewazniony`/`wygasly` w wyniku działania w UI** — oba statusy
   `certyfikat_uniewazniony`/`certyfikat_wygasly` w dialogu "Szczegóły podpisu" (wymagane przez
   `main_assumptions.md`) są osiągalne tylko dla WCZEŚNIEJ zaseedowanych certyfikatów (`cert-2`,
   `cert-3`), nigdy dla certyfikatu świeżo aktywnego w momencie podpisywania — a `podpiszPades`
   odrzuca (400) podpisanie certyfikatem nieaktywnym/wygasłym, więc te dwa statusy nie da się w
   ogóle zademonstrować na nowo podpisanym dokumencie z UI, tylko ewentualnie na dokumencie
   podpisanym wcześniej zaseedowanym certyfikatem przed jego wygaśnięciem/unieważnieniem (poza
   zasięgiem demo z bieżącą datą systemową). Model użytkownik↔certyfikat w ogóle nie istnieje w
   systemie (`certyfikat.podmiot` to DN X.509, nie login) — potwierdzone w `main_assumptions.md`
   i w komentarzu `CertyfikatController.java` w `document_api`. Dodanie nowego certyfikatu wymaga
   edycji changelogu Liquibase i restartu `sign_service` — jednorazowe działanie poza UI.
2. **Seed demo nie zawiera fizycznych plików na wolumenie `dms-data` — tylko metadane.**
   `dms_service` seeduje wiersze `szablon_jasper` (`sz-1`→`szablony/decyzja_srodowiskowa_v3.jrxml`,
   `sz-2`→`szablony/wezwanie_do_zaplaty_v1.jrxml`) i `wersja_dokumentu_dms` (`wd-1`, `wd-2` pod
   `dokumenty/d-2/...`), ale żaden `.jrxml`/`.pdf` o tych nazwach nie jest commitowany do repo czy
   kopiowany na wolumen `dms-data` przy starcie (wolumen jest czysto-Docker, inicjalizowany tylko
   przez `dms-data-init` do `chown`, bez kopiowania plików — zob. `deploy/compose.yml`).
   W efekcie: `GET /api/dokumenty/d-2/wersje/{1,2}/plik` (podgląd/pobranie seedowanego dokumentu
   `d-2` z `document_api`) i próba wygenerowania/wyrenderowania dokumentu z seedowanego szablonu
   `sz-1`/`sz-2` przez akcję zbiorową zwrócą prawdopodobnie 404 z `dms_service`
   (`wczytajPlik`/`pobierzPlikSzablonu` czytają z dysku po `sciezka_pliku`, bez fallbacku) — **do
   zweryfikowania empirycznie po starcie stosu**, ale sama analiza kodu/seedów nie potwierdza
   istnienia tych plików. Jeśli się potwierdzi: krok 1 (wgranie WŁASNEGO szablonu `.jrxml` przez
   UI) jest wtedy OBOWIĄZKOWYM pierwszym krokiem do przejścia przepływu "generowanie dokumentu",
   nie tylko opcją — seedowane szablony/dokumenty demo nie są użyteczne do manualnego testu bez
   wcześniejszego wgrania własnego pliku przez ekran Szablony.
3. **Brak akcji "wygeneruj dokument z szablonu" w samym module Dokumenty.** Jedyna droga UI do
   wywołania `jasper_service` dla nowego dokumentu to ekran `Wymiana → Akcje zbiorowe` (karta
   "Generowanie dokumentów", `apps/document_web/src/app/features/wymiana/akcje-zbiorowe/
   akcje-zbiorowe.ts`, trasa zarejestrowana w `wymiana.routes.ts`, link z listy obywateli) — z
   karty szczegółów sprawy czy listy dokumentów nie da się wprost wygenerować nowego dokumentu z
   wybranego szablonu. Działa też dla JEDNEJ zaznaczonej sprawy, więc przepływ JEST wykonywalny z
   UI, tylko nieintuicyjnie umieszczony poza modułem Dokumenty.
4. **Przycisk "Generuj wezwanie" w module Płatności jest frontendowym mockiem.**
   `platnosci-strona.ts`, metoda `potwierdzWezwanie()`: `// Mock — rzeczywiste wygenerowanie
   pisma z szablonu realizuje moduł Dokumenty` — dialog zamyka się bez żadnego wywołania HTTP.
   Endpoint `POST /api/naleznosci/{id}/wezwanie` realnie istnieje i działa po stronie
   `document_api` (`PlatnosciController`/`PlatnosciService.wygenerujWezwanie`), ale nie jest
   wołany z UI — generowanie wezwania do zapłaty jako pismo z szablonu Jasper jest więc
   NIEOSIĄGALNE z UI tą drogą (jest osiągalne przez akcje zbiorowe z dowolnym szablonem, patrz
   punkt 3, ale nie konkretnie "wezwanie dla tej należności z właściwym szablonem" jednym
   kliknięciem).
5. **Znakowanie czasem (TSA) nie ma osobnej akcji w UI — to nie jest brak.** Jest zawsze
   automatyczną częścią `POST /api/podpisy/pades` (BASELINE-T), zgodnie z kontraktem
   `sign_service.md`. Wymieniono jako potwierdzenie, że nie trzeba dodatkowego kroku poza UI.
6. **XAdES (podpis komunikatów/dokumentów XML) nie jest skonsumowany przez `document_api`
   i nie ma odpowiednika w UI** — `sign_service` wystawia `POST /api/podpisy/xades` i
   `POST /api/podpisy/weryfikacja-xades`, ale `document_api.PodpisController`-ekwiwalent
   (`DokumentController`) ich nie woła (zob. `main_assumptions.md`: "Zakres NIE zaimplementowany:
   XAdES"). Dotyczy to podpisu dla modułu Wymiana XML, nie modułu Dokumenty opisanego tutaj —
   wymieniono dla kompletności, bo `main_assumptions.md` wymienia podpis XAdES jako funkcję
   domenową ogólnosystemową.
7. **`GET /api/szablony/{id}/plik`** (pobranie treści pliku `.jrxml` samego szablonu z
   `dms_service`) nie ma odpowiednika w UI — `document_api` nie proxuje tego endpointu i
   `szablony-strona` go nie używa.

## Ocena końcowa

Najważniejsza luka: **podpisu nie da się z UI doprowadzić do statusu `certyfikat_wygasly` lub
`certyfikat_uniewazniony` na dokumencie podpisanym w ramach demo** (brak 1) — `sign_service`
odrzuca podpisanie certyfikatem już nieaktywnym/wygasłym, a nie ma żadnego sposobu z UI
(ani z API wołanego przez UI) na unieważnienie lub przedawnienie certyfikatu po fakcie, więc te
dwa z czterech wymaganych w `main_assumptions.md` statusów podpisu są wykonalne tylko na
historycznych danych seed, nigdy jako wynik działania użytkownika w aplikacji. Druga istotna
luka (potencjalnie blokująca, do zweryfikowania na żywym stosie): seedowane szablony/wersje
dokumentów w `dms_service` mają metadane bez potwierdzonych plików fizycznych na wolumenie
(brak 2) — jeśli pliki faktycznie nie istnieją, pierwsze manualne przejście przepływu wymaga
wgrania własnego szablonu `.jrxml`, zanim cokolwiek da się wygenerować/podpisać. Poza tymi dwoma
zastrzeżeniami, główny przepływ — wgranie szablonu, wygenerowanie dokumentu z danych sprawy
(przez akcje zbiorowe), podgląd PDF, podpisanie aktywnym certyfikatem z automatycznym
znakowaniem czasem, podgląd szczegółów podpisu (status OCSP/CRL, odcisk certyfikatu, podpisujący,
czas) — jest wykonywalny z poziomu UI `document_web` bez surowych zapytań REST.
