# Kontrakt: `document_api` ↔ `document_web` — moduł Administracja

Endpointy modułu Administracja, zaimplementowane po stronie `document_api`. Typy współdzielone
żyją w `packages/document-shared-dto` (`administracja.ts`, `sprawy.ts`, `dokumenty.ts`).

## Użytkownicy i role (RBAC)

- `GET /api/uzytkownicy` — lista użytkowników (login, imię, nazwisko, email, role, wydział,
  stanowisko). Filtrowanie opcjonalne po wydziale/roli.
- `POST /api/uzytkownicy` — utworzenie użytkownika (frontend obecnie dodaje rekord tylko lokalnie,
  w sygnale `uzytkownicy`).
- `PUT /api/uzytkownicy/{id}/role` — aktualizacja ról przypisanych do użytkownika (frontend edytuje
  role inline w tabeli przez `p-multiselect`).
- Uwaga: konta i uwierzytelnianie są prowadzone przez Keycloak (realm `document-workflow`) — ten
  endpoint dotyczy profilu użytkownika w `core` (wydział, stanowisko), nie zarządzania kontem OIDC.
  Sposób synchronizacji konta Keycloak ↔ profil w `core` do ustalenia przy implementacji.

## Struktura organizacyjna

- `GET /api/struktura-organizacyjna` — lista wydziałów z zagnieżdżonymi stanowiskami (nazwa
  wydziału, kierownik, lista stanowisk). Frontend renderuje to jako `p-tree`.
- `POST /api/struktura-organizacyjna/wydzialy` / `PUT .../{id}` — zarządzanie wydziałami (brak w
  obecnym zakresie frontendu — tylko podgląd na tym etapie).

## Słowniki

- `GET /api/slowniki/jrwa`, `PUT /api/slowniki/jrwa/{symbol}` — pozycje JRWA (symbol, hasło,
  kategoria archiwalna). Model współdzielony: `PozycjaJrwa` (`administracja.ts`).
- `GET /api/slowniki/typy-dokumentow`, `PUT /api/slowniki/typy-dokumentow/{id}` — typy dokumentów
  (`TypDokumentu`, `dokumenty.ts`).
- `GET /api/slowniki/dostawcy-wysylki`, `PUT /api/slowniki/dostawcy-wysylki/{id}` — dostawcy
  wysyłki (e-Doręczenia, operator pocztowy, e-mail) wraz z flagą aktywności (`DostawcaWysylki`,
  `administracja.ts`).
- `GET /api/slowniki/stawki-oplat`, `PUT /api/slowniki/stawki-oplat/{id}` — stawki opłat (nazwa,
  kwota, `StawkaOplaty`, `administracja.ts`).

## Definicje obiegów

- `GET /api/definicje-obiegow` — lista definicji (`DefinicjaObiegu`, `sprawy.ts`: nazwa, opis,
  stany, stanPoczatkowy, przejscia). Pole `przejscia` niesie model przejść między stanami
  (`PrzejscieObiegu`: zStanu, doStanow, wymaganaRola, wymaganeTypyDokumentow) — podłączone we
  frontendzie.
- `PUT /api/definicje-obiegow/{id}` — edycja nazwy/opisu definicji (stany/przejścia/role
  pozostają niezmienione przez ten endpoint).

## Dziennik zdarzeń (audit log)

- `GET /api/audit-log` — wpisy dziennika zdarzeń (data/czas, użytkownik, typ operacji, obiekt,
  szczegóły), z filtrowaniem po typie operacji i zakresie dat (`od`, `do` jako parametry zapytania).
  Model współdzielony: `WpisDziennikaZdarzen`/`TypOperacjiAudytu` (`administracja.ts`).
- Emisja wpisów jest automatyczna, wołana z serwisów innych modułów po pomyślnym wykonaniu operacji
  domenowej (w tej samej transakcji): `REJESTRACJA_SPRAWY` (`SprawaService.rejestrujSprawe`),
  `ZMIANA_STANU_SPRAWY` (`SprawaService.zmienStan`), `REJESTRACJA_WPLATY`
  (`PlatnosciService.dopasujWplate` oraz `PlatnosciService.importujCamt053` — dla importu CAMT.053
  jeden zbiorczy wpis na cały import, nie jeden na każdą wpłatę), `WYSYLKA_PISMA`
  (`KorespondencjaService.nadajPismo`), `ZMIANA_DANYCH_UZYTKOWNIKA`
  (`UzytkownikService.aktualizujRole`), `WYGENEROWANIE_WEZWANIA`
  (`PlatnosciService.wygenerujWezwanie` — tylko dla należności ze statusem `przeterminowana`).
  Te same operacje wywołane z akcji zbiorowych
  (`AkcjaZbiorowaProcessingService`) emitują wpisy analogicznie, z osobą zleceniodawcy akcji jako
  `uzytkownik`.
- Bez emisji: `PODPISANIE_DOKUMENTU` (podpis żyje w `sign_service`, niepodłączony jeszcze przez
  `document_api`) i `DEKRETACJA` (brak zamodelowanej operacji przekazania sprawy w `document_api`).
