# Przepływ: Logowanie i administracja

## Zakres

Uwierzytelnianie OIDC przez Keycloak (`document_web` ↔ realm `document-workflow`) oraz moduł
Administracja: zarządzanie profilami użytkowników i rolami domenowymi, podgląd struktury
organizacyjnej, edycja słowników (JRWA, typy dokumentów, dostawcy wysyłki, stawki opłat), edycja
nazwy/opisu definicji obiegów i przegląd dziennika zdarzeń (audit log) — z perspektywy
`document_web` (`features/auth/login`, `core/guards/auth.guard.ts`,
`features/administracja/**`), `document_api` (`UzytkownikController`, `SlownikiController`,
`StrukturaOrganizacyjnaController`, `AuditLogController`, `SprawaController` — definicje obiegów)
i Keycloak (konsola administracyjna, realm-import w `deploy/compose.yml`).

## Kroki przepływu (UI)

1. Wejście niezalogowanego użytkownika na dowolny chroniony adres — `authGuard`
   (`core/guards/auth.guard.ts`) przekierowuje na `/logowanie`. Ekran logowania
   (`features/auth/login/login.ts`) ma jeden przycisk "Zaloguj się", który woła
   `AuthService.zaloguj()` → `OAuthService.initCodeFlow()` — przekierowanie do strony logowania
   **Keycloaka** (Authorization Code + PKCE S256, `directAccessGrantsEnabled: false` — brak
   formularza login/hasło osadzonego w `document_web`, logowanie następuje wyłącznie na stronie
   Keycloaka).
2. Logowanie na stronie Keycloaka (system Keycloak, ekran logowania realmu
   `document-workflow`) — użytkownik wpisuje login/hasło konta demo lub innego konta realmu.
   Po sukcesie Keycloak przekierowuje z powrotem do `document_web` (`redirectUris` z configu
   klienta `document-web`) z kodem autoryzacji, wymienianym automatycznie na tokeny przez
   `angular-oauth2-oidc`.
3. `document_web` zasila sygnał `uzytkownik` z claimów id_tokenu (`sub`/`preferred_username`/
   `given_name`/`family_name`/`email`) i rolami z `realm_access.roles` access tokenu
   (przefiltrowanymi do pięciu ról domenowych) — żadne dodatkowe wywołanie REST nie jest
   potrzebne do zalogowania. Kolejne żądania do `document_api` dostają token automatycznie
   (interceptor `resourceServer` biblioteki OIDC) — endpoint użyty: brak (bearer token w nagłówku
   `Authorization`, walidowany przez `document_api` jako OAuth2 Resource Server).
4. Wylogowanie — przycisk w `document_web` woła `AuthService.wyloguj()` → `OAuthService.logOut()`
   — przekierowanie do endpointu wylogowania **Keycloaka**, powrót do `document_web` przez
   `post.logout.redirect.uris`.
5. Panel Administracja, ekran `/administracja` (`administracja-strona`) — linki do czterech
   podekranów opisanych poniżej; dostępność linku "Użytkownicy" nie jest warunkowana rolą w menu,
   ale ekran sam ukrywa akcje i treść dla roli innej niż `administrator` (zob. krok 6).
6. Użytkownicy i role, ekran `/administracja/uzytkownicy` (`uzytkownicy.ts`) — wymaga roli
   `administrator` (front sprawdza `auth.uzytkownik()?.role` przed wysłaniem żądania, backend
   wymusza to samo na `GET`/`POST`/`PUT` tego zasobu):
   - lista profili — `GET /api/uzytkownicy?wydzial=&rola=`,
   - dodanie nowego profilu (dialog "Dodaj użytkownika": login, imię, nazwisko, e-mail, wydział,
     stanowisko, role domenowe) — `POST /api/uzytkownicy`,
   - zmiana ról pojedynczego użytkownika (multiselect w wierszu tabeli) —
     `PUT /api/uzytkownicy/{id}/role`,
   - zmiana ról zbiorczo dla zaznaczonych wierszy (dialog "Zarządzaj rolami") — front wywołuje
     sekwencyjnie `PUT /api/uzytkownicy/{id}/role` per zaznaczony użytkownik (brak zbiorczego
     endpointu backendowego).
7. Struktura organizacyjna, ekran `/administracja/struktura` (`struktura.ts`) — drzewo
   wydział → stanowiska, wyłącznie podgląd — endpoint REST użyty:
   `GET /api/struktura-organizacyjna`.
8. Słowniki, ekran `/administracja/slowniki` (`slowniki.ts`), zakładki JRWA / typy dokumentów /
   dostawcy wysyłki / stawki opłat — przyciski edycji widoczne tylko dla roli `administrator`:
   - JRWA — `GET /api/jrwa`, edycja pozycji (dialog) — `PUT /api/jrwa/{symbol}`,
   - typy dokumentów — `GET /api/typy-dokumentow`, edycja — `PUT /api/typy-dokumentow/{id}`,
   - dostawcy wysyłki — `GET /api/dostawcy-wysylki`, edycja — `PUT /api/dostawcy-wysylki/{id}`,
   - stawki opłat — `GET /api/stawki-oplat`, edycja — `PUT /api/stawki-oplat/{id}`.
9. Obiegi (definicje procesów), ekran `/administracja/obiegi` (`obiegi.ts`):
   - lista i podgląd stanów/przejść danej definicji (dialog "Szczegóły") —
     `GET /api/definicje-obiegow`,
   - edycja nazwy/opisu (dialog "Edytuj", widoczny tylko dla roli `administrator`) —
     `PUT /api/definicje-obiegow/{id}`.
10. Dziennik zdarzeń, ekran `/administracja/dziennik-zdarzen` (`dziennik-zdarzen.ts`) — filtr typu
    operacji i zakresu dat, wyłącznie podgląd — endpoint REST użyty:
    `GET /api/audit-log?typOperacji=&od=&do=`.
11. Zarządzanie samym kontem Keycloak (hasło, włączenie/wyłączenie konta, przypisanie roli
    realmu, które faktycznie autoryzuje operacje w `document_api`) — **wyłącznie w konsoli
    administracyjnej Keycloaka** (`http://localhost:10681`, zalogowanej kontem z
    `KEYCLOAK_ADMIN_USERNAME`/`KEYCLOAK_ADMIN_PASSWORD`), nie w `document_web`. Jest to akceptowalne
    w ramach architektury (Keycloak ma własny UI administracyjny), ale to jest inny system niż
    `document_web` i inny model danych niż profil `core.uzytkownik` z kroku 6.

## Dane testowe do manualnego przejścia przepływu

Konfiguracja OIDC po stronie `document_web` (`src/environments/environment.ts`): issuer
`http://localhost:10681/realms/document-workflow`, `clientId` = `document-web`,
`documentApiBaseUrl` = `http://localhost:10620`.

Użytkownicy demo zaimportowani do realmu Keycloak `document-workflow`
(`deploy/compose.yml`, `configs.keycloak-realm`) — hasło wspólne dla wszystkich czterech,
ze zmiennej `KEYCLOAK_DEMO_USER_PASSWORD` (domyślnie `change-me` w `.env.example`, **musi** być
zmienione na serwerze produkcyjnym):

| Login                | Rola realmu     | Imię i nazwisko (profil `core`) |
| -------------------- | --------------- | ------------------------------- |
| `demo.administrator` | `administrator` | Adam Nowakowski                 |
| `demo.referent`      | `referent`      | Katarzyna Zielińska             |
| `demo.kierownik`     | `kierownik`     | Piotr Lewandowski               |
| `demo.ksiegowosc`    | `ksiegowosc`    | Marta Wiśniewska                |

Konsola administracyjna Keycloaka: login `KEYCLOAK_ADMIN_USERNAME` (domyślnie `admin`), hasło
`KEYCLOAK_ADMIN_PASSWORD` (domyślnie `change-me` w `.env.example`), adres
`KEYCLOAK_PUBLIC_URL` (domyślnie `http://localhost:10681`).

Struktura organizacyjna seedowana w `core` (Liquibase `demo/100-seed.xml`):

- Wydział Ochrony Środowiska (kierownik Katarzyna Zielińska) — stanowiska: Kierownik wydziału,
  Referent, Starszy referent.
- Wydział Organizacyjny (kierownik Piotr Lewandowski) — stanowiska: Kierownik wydziału,
  Pracownik kancelarii, Administrator systemu, Sekretarz.
- Wydział Księgowości (kierownik Marta Wiśniewska) — stanowiska: Główny księgowy, Specjalista
  ds. księgowości, Kasjer.
- Wydział Komunikacji (kierownik Jan Kamiński) — stanowiska: Kierownik wydziału, Zastępca
  kierownika, Referent ds. rejestracji pojazdów.

Przykładowe pozycje JRWA (do edycji w kroku 8): symbol `1510` — "Skargi i wnioski" (kat. B5);
symbol `3120` — "Faktury i rachunki" (kat. B5); symbol `6220` — "Decyzje o środowiskowych
uwarunkowaniach" (kat. A). Przykładowy dostawca wysyłki: `dw-1` "e-Doręczenia" (typ
`e-doreczenia`, aktywny). Przykładowa stawka opłaty: `s-1` "Opłata skarbowa — pełnomocnictwo",
17,00 zł.

## Braki / działania niemożliwe z UI (document_web + Keycloak)

1. **Profile `core.uzytkownik` zarządzane w `document_web` i konta Keycloak są dwoma
   rozłącznymi zbiorami danych, synchronizowanymi tylko ręcznie.**
   `UzytkownikController` (`apps/document_api/.../controller/UzytkownikController.java:24-25`,
   komentarz: _"konta/uwierzytelnianie prowadzi Keycloak, bez integracji z jego API
   administracyjnym w tym miejscu"_) operuje wyłącznie na profilu w schemie `core` (login,
   wydział, stanowisko, role domenowe) — `POST /api/uzytkownicy` **nie tworzy** konta w Keycloaku i
   `PUT /api/uzytkownicy/{id}/role` **nie zmienia** roli realmu Keycloak. Autoryzacja operacji
   mutujących w `document_api` (`SecurityConfig.filterChain`, `hasRole`/`hasAnyRole`) opiera się
   wyłącznie o `realm_access.roles` z tokenu JWT wydanego przez Keycloak — rola widoczna/edytowana
   w ekranie "Użytkownicy" `document_web` **nie wpływa** na to, co dany użytkownik faktycznie może
   zrobić. Dowód materialny: seed `demo/100-seed.xml` zawiera profil `u-5` (login
   `demo.kancelaria`, rola domenowa `kancelaria`) w `core.uzytkownik`, ale realm-import w
   `deploy/compose.yml` **nie** zawiera żadnego użytkownika Keycloak o nazwie `demo.kancelaria` —
   ten profil jest widoczny na liście w `document_web`, lecz nikt nie może się nim zalogować, dopóki
   administrator nie utworzy odpowiadającego konta ręcznie w konsoli Keycloaka. Dodanie nowego,
   realnie działającego użytkownika wymaga więc dwóch oddzielnych, niepowiązanych kroków: konto +
   rola realmu w konsoli Keycloaka (akceptowalne — własny UI systemu) oraz profil w
   `document_web` (login musi być wpisany identycznie w obu miejscach ręcznie, bez żadnej
   walidacji zgodności między nimi).
2. **Struktura organizacyjna (wydziały, stanowiska) nie ma żadnego CRUD — ani w `document_web`,
   ani w Keycloaku.** `StrukturaOrganizacyjnaController` wystawia wyłącznie
   `GET /api/struktura-organizacyjna` (potwierdzone też w
   `docs/development_docs/currentwork/contracts/document_api_administracja.md`). Brak
   `POST`/`PUT`/`DELETE` w backendzie i brak formularza w `features/administracja/struktura`
   (komponent renderuje `TreeNode[]` z odpowiedzi GET, bez żadnej akcji edycji). Dodanie nowego
   wydziału/stanowiska lub zmiana kierownika jest możliwa wyłącznie przez bezpośredni insert/update
   SQL w tabelach `wydzial`/`stanowisko` schemy `core` (lub nowy changeset Liquibase) — **realna
   luka, nie "wymaga konsoli Keycloaka"**, bo dane te nie istnieją w Keycloaku.
3. **Nowych pozycji słownikowych (JRWA, typ dokumentu, dostawca wysyłki, stawka opłaty) nie można
   dodać z UI — tylko edytować istniejące.** `SlownikiController` i `SprawaController` wystawiają
   tylko `GET`/`PUT` dla tych czterech zasobów (`/api/jrwa/{symbol}`,
   `/api/typy-dokumentow/{id}`, `/api/dostawcy-wysylki/{id}`, `/api/stawki-oplat/{id}`) — brak
   `POST` w kontrolerze i brak przycisku "Dodaj" w `features/administracja/slowniki` (tylko
   "Edytuj" per wiersz istniejącej listy). Nowa pozycja JRWA, nowy typ dokumentu, nowy dostawca
   wysyłki czy nowa stawka opłaty wymaga nowego changesetu Liquibase lub bezpośredniego insertu
   SQL — realna luka.
4. **Nowej definicji obiegu nie można utworzyć z UI, a istniejącą można edytować tylko w zakresie
   nazwy/opisu — nie stanów/przejść/ról.** `EdytujDefinicjeObieguRequest` ma tylko pola `nazwa` i
   `opis` (`apps/document_api/.../dto/administracja/EdytujDefinicjeObieguRequest.java`);
   `SprawaController` wystawia `GET /api/definicje-obiegow` i
   `PUT /api/definicje-obiegow/{id}` — brak `POST` (nowa definicja) i brak endpointu do zmiany
   stanów/przejść/wymaganych ról/wymaganych dokumentów istniejącej definicji. Ekran
   `features/administracja/obiegi` (`obiegi.ts`) ma dialog "Edytuj" tylko z polami nazwa/opis,
   zgodnie z backendem — stany i przejścia są renderowane tylko do odczytu w dialogu "Szczegóły".
   `main_assumptions.md` (sekcja "Administracja"/sekcja 1) zakłada, że "nowy rodzaj sprawy ma
   własną definicję obiegu bez zmian w kodzie maszyny stanów" — w praktyce nowa definicja (lub
   zmiana stanów/przejść istniejącej) wymaga dopisania changesetu Liquibase, nie jest to dostępne z
   żadnego UI.
5. **Profilu użytkownika (`core.uzytkownik`) nie można edytować (wydział/stanowisko/dane
   osobowe) ani usunąć/deaktywować z UI po utworzeniu.** `UzytkownikController` wystawia tylko
   `GET`, `POST` i `PUT .../role` — nie ma `PUT`/`PATCH` do zmiany wydziału/stanowiska/imienia/
   nazwiska/e-maila istniejącego profilu i nie ma `DELETE`. Jedyna mutacja istniejącego wiersza to
   zestaw ról. Zablokowanie logowania jest możliwe (w Keycloaku — wyłączenie konta), ale profil w
   `document_web` zostaje na liście bez żadnego oznaczenia nieaktywności.

**Najważniejsza luka:** punkt 1 — rola widoczna i edytowana w ekranie "Użytkownicy"
`document_web` jest kosmetyczna (profil domenowy), a nie rzeczywistym mechanizmem autoryzacji;
realna kontrola dostępu (role realmu Keycloak w `realm_access.roles` tokenu) żyje wyłącznie w
Keycloaku i nie jest zsynchronizowana z `document_web` żadnym mechanizmem automatycznym — to, co
administrator widzi i edytuje w ekranie "Użytkownicy", nie odpowiada temu, co dana osoba
faktycznie może zrobić w systemie. Do tego dochodzą punkty 2–4 (brak CRUD struktury, brak
tworzenia nowych pozycji słownikowych/definicji obiegów z UI) — każde z nich wymaga Liquibase/SQL,
nie tylko konsoli Keycloaka.
