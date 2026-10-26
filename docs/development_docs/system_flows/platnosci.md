# Przepływ: Rozliczanie płatności

## Zakres

Przepływ obejmuje: naliczenie/rejestr należności powiązanych ze sprawą, rejestr wpłat, import
wyciągu bankowego CAMT.053 z automatycznym dopasowaniem wpłaty do należności, ręczne dopasowanie
wpłaty niedopasowanej, generowanie przypomnienia/wezwania do zapłaty dla należności przeterminowanej
oraz podgląd rozrachunków strony (zakładka "Rozrachunki" karty obywatela). Backend: moduł Płatności
w `document_api` (`PlatnosciController`/`PlatnosciService`, schema `core`, tabele `naleznosc`/
`wplata`). Frontend: `apps/document_web/src/app/features/platnosci/**` (rejestr należności, rejestr
wpłat, import wyciągu, szczegóły należności) oraz zakładka "Rozrachunki" w module Obywatele.
Integracja z `jasper_service`: tylko po stronie backendu, przez `PlatnosciService.wygenerujWezwanie`
(render PDF wezwania + zapis nowej wersji dokumentu przez `dms_service`) — **nie jest jeszcze
podłączona z frontu**, zob. sekcja "Braki" poniżej.

## Kroki przepływu (UI)

1. Rejestr należności, ekran `/platnosci` (`platnosci-strona`) — filtr statusu
   (nierozliczona/częściowo rozliczona/rozliczona/przeterminowana), tabela z kolumnami znak sprawy,
   kontrahent, numer referencyjny, kwota, pozostało do zapłaty — endpoint: `GET /api/naleznosci?status=...`
2. Podgląd szczegółów jednej należności, ekran `/platnosci/:id` (`naleznosc-szczegoly`) — dane
   należności, procent rozliczenia, lista powiązanych wpłat, znak sprawy i dane obywatela
   (dociągane dodatkowymi wywołaniami) — endpointy: `GET /api/naleznosci/:id`,
   `GET /api/wplaty?naleznoscId=...`, `GET /api/sprawy/:id`, `GET /api/obywatele/:id`.
3. Podgląd rozrachunków strony w ramach karty obywatela, zakładka "Rozrachunki" — lista należności i
   wpłat danego obywatela — endpointy: `GET /api/naleznosci?obywatelId=...`,
   `GET /api/wplaty?obywatelId=...`.
4. Rejestr wpłat, ekran `/platnosci/wplaty` (`wplaty-lista`) — wszystkie wpłaty wraz z informacją,
   czy dopasowana automatycznie — endpoint: `GET /api/wplaty`.
5. Import wyciągu bankowego CAMT.053, ekran `/platnosci/import-wyciagu` (`import-wyciagu`) —
   upload pliku XML (multipart, pole `plik`), backend parsuje wpisy kredytowe (`CdtDbtInd=CRDT`),
   próbuje dopasować automatycznie (po numerze referencyjnym w tytule/referencji przelewu lub po
   dokładnej kwocie brakującej do rozliczenia — tylko gdy trafia DOKŁADNIE jeden kandydat), tworzy
   wpłaty i zwraca podsumowanie (liczba transakcji / dopasowanych automatycznie / do rekoncyliacji) —
   endpoint: `POST /api/import-camt053`. Wynik dopasowania jest widoczny w UI (tabela zaimportowanych
   wpłat z flagą dopasowania), nie jest czarną skrzynką.
6. Ręczne dopasowanie wpłaty niedopasowanej, dialog w `wplaty-lista` ("Dopasuj do należności") —
   użytkownik wybiera wpłatę bez przypisanej należności, z listy nierozliczonych/częściowo
   rozliczonych należności wybiera właściwą, backend przelicza `kwotaRozliczona`/status należności
   — endpoint: `POST /api/wplaty/:id/dopasuj` (body `{ naleznoscId }`).
7. Generowanie wezwania do zapłaty dla należności przeterminowanej — przycisk w `platnosci-strona`
   otwiera dialog potwierdzenia, ale **dialog jest mockiem** (`potwierdzWezwanie()` tylko zamyka
   dialog, nie wywołuje żadnego żądania HTTP) — zob. sekcja "Braki" poniżej. Backendowy endpoint
   istnieje: `POST /api/naleznosci/:id/wezwanie` (zwraca metadane utworzonego `Dokument`u; 400 jeśli
   status należności różny od `przeterminowana`), ale nie jest wywoływany z UI.

## Dane testowe do manualnego przejścia przepływu

Repo zawiera realne dane demo (`apps/document_api/src/main/resources/db/changelog/demo/100-seed.xml`,
changeset `100-14-naleznosci`/`100-15-wplaty`, kontekst `demo`) i plik testowy CAMT.053
(`apps/document_api/src/test/resources/camt053/wyciag-dopasowanie.xml`), które się wzajemnie
pokrywają — dobre do manualnego przejścia przepływu import → dopasowanie:

- Należność `n-2`: numer referencyjny **`FK/2026/0088`**, tytuł "Usługi serwisowe — faktura",
  sprawa `spr-3`, obywatel `ob-3`, kwota **4 500,00 PLN**, `kwotaRozliczona = 0`, termin płatności
  **2026-10-30**, status `nierozliczona`.
- Należność `n-1`: numer referencyjny `OPL/2026/5541`, tytuł "Opłata skarbowa", 17,00 PLN, już
  `rozliczona` (wpłata `w-1`, kontrahent "Jan Kowalski", data 2026-09-09).
- Wpłata `w-2`: 250,00 PLN, data 2026-10-18, tytuł "Wpłata niezidentyfikowana", kontrahent
  "Nieznany", `dopasowana_automatycznie = false` — gotowa do przejścia ścieżki ręcznego dopasowania
  (krok 6 powyżej) w `wplaty-lista`.
- Plik `wyciag-dopasowanie.xml` (CAMT.053, `camt.053.001.02`) ma trzy wpisy (`Ntry`):
  1. **CRDT 4500,00 PLN**, data księgowania 2026-10-21, dłużnik "Budimex Serwis Sp. z o.o.",
     `RmtInf/Ustrd` = "Zapłata faktury FK/2026/0088" — referencja zawiera numer `FK/2026/0088`,
     czyli dopasuje się automatycznie do istniejącej należności `n-2` (dokładnie jeden kandydat).
  2. CRDT 999,99 PLN, dłużnik "Firma Zagadka s.c.", tytuł bez odniesienia do żadnej należności —
     zostaje niedopasowana (0 kandydatów), trafia do rekoncyliacji ręcznej.
  3. DBIT 12,00 PLN ("Opłata bankowa") — pomijana, bo backend importuje tylko wpisy kredytowe.
- Do testu rachunku bankowego w formacie IBAN PL (plik testowy go nie zawiera — parser czyta tylko
  `Amt`/`CdtDbtInd`/`BookgDt`/`RmtInf`/`AddtlNtryInf`/`NtryRef`/`Dbtr/Nm`, nie `IBAN`/`Acct`) można
  użyć realnego numeru w formacie `PL61 1090 1014 0000 0712 1981 2874` jako danych pomocniczych przy
  przygotowywaniu własnego pliku wyciągu do testu manualnego — struktura `camt.053.001.02` dopuszcza
  go w `Stmt/Acct/Id/IBAN`, ale import go nie odczytuje i nie waliduje.

## Braki / działania niemożliwe z UI

1. **Naliczenie należności nie ma formularza w UI ani odpowiadającego endpointu REST w
   `document_api`.** `PlatnosciController` wystawia tylko `GET /api/naleznosci`,
   `GET /api/naleznosci/:id`, `POST /api/naleznosci/:id/wezwanie` — nie ma żadnego
   `POST /api/naleznosci` (tworzenie nowej należności). W całym `document_api` nie istnieje
   wywołanie `new NaleznoscEntity(...)` poza mapperem testowym/seedem Liquibase — jedyny sposób na
   wprowadzenie nowej należności do systemu to insert w changelogu Liquibase
   (`db/changelog/demo/100-seed.xml`, changeset `100-14-naleznosci`) lub inna migracja bazy danych,
   czyli poza aplikacjami z `./apps` i poza UI. "Naliczanie należności powiązanych ze sprawą (...) z
   numerem referencyjnym płatności" z `main_assumptions.md` (sekcja "Rozliczanie płatności") nie jest
   więc zaimplementowane — ani jako akcja UI, ani jako endpoint backendowy.
2. **Generowanie wezwania do zapłaty jest mockiem w UI, mimo że backend jest gotowy.** Endpoint
   `POST /api/naleznosci/:id/wezwanie` istnieje i działa (`PlatnosciController.java:49-52`,
   `PlatnosciService.wygenerujWezwanie`, orkiestracja `jasper_service` + `dms_service`), ale
   `platnosci-strona.ts` (`potwierdzWezwanie()`, linia ~93-97) tylko zamyka dialog — nie wywołuje
   `PlatnosciApi`. `PlatnosciApi` (`platnosci.api.ts`, komentarz linii 23-31) w ogóle nie wystawia
   metody dla tego endpointu, z komentarzem stwierdzającym (już nieaktualnie), że endpoint "nie
   istnieje po stronie backendu" — w rzeczywistości endpoint powstał później (commit `fd1bfbf`,
   po commicie `887b152` podłączającym frontend), a frontend nie został zaktualizowany. Potwierdzone
   `git log` na obu plikach: `PlatnosciController.java` ma commit `fd1bfbf` ("Wezwanie do zapłaty —
   nowy endpoint w document_api") PO commicie `887b152` ("document_web: moduł Płatności podłączony
   do rzeczywistego document_api"). Oznacza to, że manualny trigger wezwania z UI jest obecnie
   niemożliwy — jedyny sposób wywołania endpointu to bezpośrednie zapytanie REST (`curl`/Postman),
   co jest poza zakresem dozwolonych działań z UI.
3. Automatyczne dopasowanie wpłaty do należności przy imporcie CAMT.053 NIE jest czarną skrzynką —
   wynik (liczba dopasowanych automatycznie / do rekoncyliacji, lista wpłat z flagą dopasowania)
   jest w pełni widoczny w UI po imporcie. Brak nie dotyczy tego punktu.
4. Ręczne powiązanie niedopasowanej wpłaty z należnością MA akcję w UI (`wplaty-lista`, dialog
   "Dopasuj do należności", `POST /api/wplaty/:id/dopasuj`) — brak nie dotyczy tego punktu.

**Podsumowanie:** przepływ NIE da się wykonać w całości z poziomu UI. Dwa konkretne braki:
naliczenie nowej należności (brak w ogóle, backend i UI) oraz manualne wygenerowanie wezwania do
zapłaty (backend gotowy, UI świadomie mockuje akcję — udokumentowane w `main_assumptions.md` jako
odłożone do modułu Dokumenty, ale wciąż niezrealizowane).
