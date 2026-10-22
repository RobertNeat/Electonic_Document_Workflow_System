# Kontrakt: `document_api` ↔ `document_web` — moduł Administracja

Endpointy wymagane przez `src/app/features/administracja/` we frontendzie. Obecnie moduł działa w
całości na danych mockowych (lokalnie w `features/administracja/mock/admin-mock-data.ts` oraz na
danych z `core/mock/mock-data.ts`) — poniższe endpointy nie są jeszcze zaimplementowane po stronie
`document_api`.

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
  kategoria archiwalna). Model współdzielony z `core/models/jrwa.model.ts` (`PozycjaJrwa`).
- `GET /api/slowniki/typy-dokumentow`, `PUT /api/slowniki/typy-dokumentow/{id}` — typy dokumentów
  (`TypDokumentu` z `core/models/dokument.model.ts`).
- `GET /api/slowniki/dostawcy-wysylki`, `PUT /api/slowniki/dostawcy-wysylki/{id}` — dostawcy
  wysyłki (e-Doręczenia, operator pocztowy, e-mail) wraz z flagą aktywności. Model lokalny
  `DostawcaWysylki` we frontendzie (`features/administracja/models/administracja.model.ts`) —
  wymaga odpowiednika w `core` lub w `delivery_mock`.
- `GET /api/slowniki/stawki-oplat`, `PUT /api/slowniki/stawki-oplat/{id}` — stawki opłat (nazwa,
  kwota). Model lokalny `StawkaOplaty` we frontendzie.

## Definicje obiegów

- `GET /api/definicje-obiegow` — lista definicji (`DefinicjaObiegu` z
  `core/models/sprawa.model.ts`: nazwa, opis, stany, stanPoczatkowy). Frontend aktualnie nie
  pokazuje przejść między stanami (pole `stany: string[]` nie zawiera informacji o przejściach,
  rolach ani wymaganych dokumentach na etapie) — do rozszerzenia modelu `DefinicjaObiegu`, gdy
  `document_api` zaimplementuje pełną maszynę stanów (zob. `plan.md`, sekcja workflow obiegów).
- `PUT /api/definicje-obiegow/{id}` — edycja nazwy/opisu definicji (frontend ma mockowy dialog
  edycji — tylko nazwa i opis, bez edycji stanów/przejść/ról).

## Dziennik zdarzeń (audit log)

- `GET /api/audit-log` — wpisy dziennika zdarzeń (data/czas, użytkownik, typ operacji, obiekt,
  szczegóły), z filtrowaniem po typie operacji i zakresie dat (`od`, `do` jako parametry zapytania).
  Model lokalny `WpisDziennikaZdarzen` we frontendzie — do przeniesienia do `core/models`, gdy
  `document_api` zacznie emitować realne zdarzenia audytowe dla operacji na sprawach, dokumentach
  i płatnościach.
