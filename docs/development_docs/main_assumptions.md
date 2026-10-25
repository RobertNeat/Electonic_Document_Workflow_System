# Główne założenia systemu

System klasy enterprise do elektronicznego obiegu dokumentów i rozliczania płatności, wzorowany na
systemach EZD (Elektroniczne Zarządzanie Dokumentacją) stosowanych w jednostkach administracji publicznej.

## 1. Funkcje dostępne dla użytkownika

### Obieg dokumentów i spraw

- System definiuje **wiele rodzajów obiegów spraw** (np. obieg wniosku o wydanie decyzji administracyjnej,
  obieg skargi/wniosku, obieg faktury kosztowej, obieg wewnętrznego pisma okólnego) — każdy obieg to osobna
  definicja: zestaw dozwolonych stanów, przejść między nimi, ról uczestniczących oraz typów dokumentów
  wymaganych na danym etapie.
- Każdy obieg może wykorzystywać kilka **typów dokumentów**, a ten sam typ dokumentu (np. „pełnomocnictwo”,
  „potwierdzenie opłaty”) może występować w wielu różnych obiegach — typy dokumentów są zdefiniowane
  niezależnie od obiegu i przypisywane do niego jako wymagane/opcjonalne na danym etapie.
- Definicja obiegu (stany, przejścia, wymagane dokumenty, role) jest konfigurowalna przez administratora, a
  nie zakodowana na trwałe dla jednego procesu — nowy rodzaj sprawy ma własną definicję obiegu bez zmian w
  kodzie maszyny stanów.
- Rejestracja sprawy z automatycznym nadaniem znaku sprawy zgodnie z JRWA (Jednolity Rzeczowy Wykaz Akt) i
  przypisaniem do konkretnego obiegu (typu procesu).
- Prowadzenie metryki sprawy (historia czynności, osoby odpowiedzialne, terminy).
- Dekretacja dokumentów przychodzących do właściwego wydziału/referenta.
- Zmiana stanu sprawy zgodnie z definicją obiegu przypisanego do sprawy, z kontrolą dozwolonych przejść i
  wymaganych uprawnień/dokumentów na danym etapie.
- Rejestr przesyłek wpływających i wychodzących (korespondencja papierowa i elektroniczna).
- Pilnowanie terminów ustawowych (np. terminy z KPA) wraz z powiadomieniami o zbliżającym się/przekroczonym
  terminie.
- Kwalifikacja archiwalna dokumentów (kategorie A/B z okresem przechowywania) i obsługa przekazania do
  archiwum zakładowego po zamknięciu sprawy.

### Zarządzanie dokumentami i szablonami (DMS)

- Wgrywanie i wersjonowanie szablonów JasperReports (`.jrxml`) dla poszczególnych typów dokumentów/pism.
- Generowanie dokumentu na podstawie szablonu i danych sprawy (wydruk do PDF).
- Przeglądanie historii wersji dokumentu danego typu wraz z metadanymi (autor, data, powiązana sprawa).
- Rozróżnienie i przeglądanie wersji dokumentu: niepodpisana (robocza) oraz podpisana (finalna, niemutowalna).
- Podgląd dokumentu (PDF) bez konieczności pobierania pliku.

### Podpis elektroniczny

- Podpisywanie dokumentów wygenerowanych z szablonów (PDF – PAdES) oraz dokumentów/komunikatów XML
  (XAdES) z użyciem certyfikatu użytkownika (demonstracyjna infrastruktura PKI, patrz sekcja 4).
- Weryfikacja statusu podpisu (poprawny / niepoprawny / certyfikat wygasły / certyfikat unieważniony).
- Znakowanie czasem (TSA) dla podpisanych dokumentów.
- Podgląd szczegółów podpisu (podpisujący, czas, odciosk certyfikatu, status OCSP/CRL).

### Wysyłka pism i korespondencja elektroniczna

- Wysyłka podpisanego pisma do adresata (mieszkańca/podmiotu) przez wybranego dostawcę:
  - e-Doręczenia (ADE – Adres do Doręczeń Elektronicznych; dowód wysłania/otrzymania zgodnie z ideą
    ADE/BAE),
  - operator pocztowy – przesyłka hybrydowa (druk + nadanie listu poleconego, ZPO),
  - e-mail jako kanał pomocniczy (bez skutków prawnych doręczenia).
- Śledzenie statusu wysłanego pisma (nadane, doręczone, odebrane, zwrot, awizo).
- Elektroniczna wymiana dokumentów w formacie XML z podmiotami zewnętrznymi (import/eksport komunikatów
  wg zdefiniowanego schematu XSD, zgodnie z zasadami EZD opisanymi w sekcji 2).
- Import/eksport faktur i dokumentów kosztowych w formacie UBL (PEPPOL) jako elektroniczna wymiana
  dokumentów typu EDI z kontrahentami/dostawcami (patrz sekcja 2).

### Rozliczanie płatności

- Naliczanie należności powiązanych ze sprawą (np. opłata skarbowa, opłata administracyjna) z numerem
  referencyjnym płatności.
- Rejestr należności i wpłat wraz ze statusem rozliczenia (nierozliczona / częściowo rozliczona /
  rozliczona / przeterminowana).
- Automatyczne/manualne powiązanie wpłaty z należnością na podstawie numeru referencyjnego, tytułu wpłaty
  lub dopasowania kwota+kontrahent.
- Import wyciągów bankowych (format CAMT.053) i zasilanie rejestru wpłat.
- Przypomnienia/wezwania do zapłaty generowane jako pismo (szablon Jasper) dla należności przeterminowanych.
- Podgląd rozrachunków strony (mieszkańca/podmiotu) w ramach danej sprawy.

### Administracja

- Zarządzanie użytkownikami, rolami i uprawnieniami (RBAC) oraz strukturą organizacyjną (wydziały,
  stanowiska).
- Konfiguracja słowników (JRWA, typy dokumentów, dostawcy wysyłki, stawki opłat).
- Przegląd dziennika zdarzeń (audit log) dla operacji na sprawach, dokumentach i płatnościach.

## 2. Standardy do zaimplementowania

Standardy istotne dla alignowania projektu z sektorem księgowo-administracyjnym w Polsce/UE. Dla każdego
określono, czy jest realizowany w pełni, czy w sposób zmockowany (funkcjonalnie zbliżony, bez prawnej
wiążącości/pełnej zgodności z realną instytucją).

| Standard                                         | Zakres                                                                                                                        | Sposób realizacji                                                                                                                                           |
| ------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **JRWA**                                         | Jednolity Rzeczowy Wykaz Akt — klasyfikacja spraw i teczek aktowych                                                           | W pełni — słownik JRWA jako dane referencyjne, znak sprawy budowany na jego podstawie.                                                                      |
| **KPA — terminy i doręczenia**                   | Terminy ustawowe, zasady doręczeń, fikcja doręczenia                                                                          | W pełni funkcjonalnie (logika terminów/przypomnień), bez pełnej wykładni prawnej przypadków granicznych.                                                    |
| **Kategorie archiwalne (A/B)**                   | Kwalifikacja archiwalna dokumentacji i okresy przechowywania                                                                  | W pełni — pole kategorii archiwalnej na dokumencie/sprawie, zgodne z JRWA.                                                                                  |
| **EZD (Elektroniczne Zarządzanie Dokumentacją)** | Zasady obiegu spraw/dokumentów elektronicznych w administracji (metryka sprawy, rejestr przesyłek, dekretacja, teczka sprawy) | W pełni funkcjonalnie jako architektura `document_api` + DMS — bez certyfikacji/zgodności z konkretnym systemem klasy EZD RP.                               |
| **e-Doręczenia (ADE/BAE)**                       | Doręczenia elektroniczne o skutku prawnym doręczenia listu poleconego za potwierdzeniem odbioru                               | Zmockowane w `delivery_mock` — adres ADE, dowody wysłania/otrzymania na wzór ADE/BAE, bez integracji z realną bazą adresów elektronicznych.                 |
| **XAdES / PAdES (ETSI)**                         | Podpis elektroniczny dla XML/PDF                                                                                              | W pełni (biblioteka EU DSS) na demonstracyjnej infrastrukturze PKI — podpis funkcjonalnie poprawny, ale certyfikaty nie są wydane przez realne, zaufane CA. |
| **RFC 3161 (TSA)**                               | Znakowanie czasem                                                                                                             | Zmockowane — własny serwer TSA zgodny ze standardem, niepubliczny/niekwalifikowany.                                                                         |
| **UBL 2.1 / PEPPOL (EDI)**                       | Elektroniczna wymiana dokumentów handlowych/kosztowych (faktury)                                                              | Zmockowane — obsługa formatu UBL jako format wymiany, bez podłączenia do realnej sieci PEPPOL (brak Access Pointa).                                         |
| **CAMT.053 (ISO 20022)**                         | Elektroniczny wyciąg bankowy                                                                                                  | W pełni — parsowanie i import rzeczywistego formatu CAMT.053.                                                                                               |
| **OIDC / OAuth2**                                | Uwierzytelnianie i autoryzacja                                                                                                | W pełni — Keycloak jako dostawca identity (patrz sekcja 4).                                                                                                 |

## 3. Integracje między systemami

### document_api ↔ dms_service

- Cel: udostępnienie modułowi głównemu operacji na dokumentach, szablonach i ich wersjach bez
  przechowywania plików binarnych w `document_api`.
- Zakres zaimplementowany: pobranie bajtów istniejącej wersji (`GET .../wersje/{wersja}/plik`) i
  rejestracja nowej wersji (`POST .../wersje`) - wywoływane przez `DmsServiceClient` w ramach
  orkiestracji podpisania dokumentu (`DokumentService.podpiszDokument`).
- Zakres NIE zaimplementowany: upload/listowanie szablonów Jasper wraz z wersjami (`/api/szablony*`)
  - `document_api` czyta je tylko z własnej schemy `core` (metadane zreplikowane przy innej okazji,
    poza tym zadaniem), nie woła jeszcze do `dms_service` dla szablonów.
- Charakter: REST/JSON, synchroniczne wywołania inicjowane przez `document_api`; `dms_service` nie woła
  z własnej inicjatywy do `document_api`. Brak uwierzytelnienia - `dms_service` nie ma Spring Security
  na classpath, więc klient używa zwykłego `RestClient` bez żadnej warstwy OAuth2.

### document_api ↔ jasper_service

- Cel: wygenerowanie dokumentu (PDF) na podstawie szablonu i danych przekazanych z `document_api`.
- Zakres: `JasperServiceClient.renderuj` woła `POST /api/render/{szablonId}` (parametry raportu =
  płaska mapa zbudowana z danych sprawy/obywatela) → odpowiedź to bajty PDF. `jasper_service` jest
  bezstanowy i nie zapisuje wyniku do `dms_service` - zapis wynikowego PDF jako nowej wersji
  dokumentu wykonuje `document_api` sam przez `DmsServiceClient.uploadNowaWersja`, analogicznie do
  orkiestracji podpisania dokumentu. Aktualny konsument: akcja zbiorowa "generowanie dokumentów"
  (`AkcjaZbiorowaProcessingService.wykonajGenerowanieDokumentu`).
- Charakter: REST/JSON, synchroniczne, bezstanowe po stronie `jasper_service` (silnik renderujący, nie
  przechowuje danych).

### document_api ↔ dostawcy wysyłki pism (e-Doręczenia mock, operator pocztowy)

- Cel: nadanie pisma do adresata i śledzenie statusu doręczenia.
- Zakres: nadanie przesyłki (dokument podpisany + metadane adresata) → numer referencyjny przesyłki;
  zmiana statusu (nadane/doręczone/odebrane/zwrot/awizo) propagowana z `delivery_mock`.
- Charakter: nadanie przesyłki jako REST/JSON (wywołanie `document_api` → zapis lokalny, bez
  wywołania do `delivery_mock` przy nadaniu); status zwrotny jako asynchroniczne zdarzenie RabbitMQ
  publikowane przez `delivery_mock` i konsumowane przez `document_api` (zob. sekcja RabbitMQ poniżej).

### document_api ↔ sign_service (podpis elektroniczny)

- Cel: złożenie i weryfikacja podpisu elektronicznego na dokumencie PDF/XML.
- Zakres zaimplementowany: podpisanie PAdES (`POST /api/podpisy/pades`) jako część orkiestracji
  `DokumentService.podpiszDokument` - `document_api` pobiera bajty aktualnej (niepodpisanej) wersji
  z `dms_service`, przekazuje je do `sign_service` z `certyfikatId` podanym explicite przez
  wołającego (brak w `document_api` modelu użytkownik↔certyfikat), a podpisany wynik zapisuje w
  `dms_service` jako nową wersję i lokalnie jako nowy wiersz `core.wersja_dokumentu`
  (`podpisana=true`, `statusPodpisu='poprawny'` - bezpieczne bez dodatkowej weryfikacji, bo
  `sign_service` odrzuca certyfikat nieaktywny/wygasły przed podpisaniem, zob. `PodpisService`).
- Zakres NIE zaimplementowany: XAdES (`/xades`, `/weryfikacja-xades`), weryfikacja podpisu PAdES
  istniejącego dokumentu (`/weryfikacja`), odczyt metadanych certyfikatów (`/api/certyfikaty*`).
- Charakter: REST/JSON, synchroniczne. Usługa korzysta z demonstracyjnej infrastruktury PKI (CA, TSA,
  responder OCSP/CRL) opisanej w sekcji 4. Brak uwierzytelnienia - `sign_service` nie ma Spring Security
  na classpath, więc klient używa zwykłego `RestClient` bez żadnej warstwy OAuth2.

### document_api ↔ payment_import (import płatności)

- Cel: zasilenie rejestru należności/wpłat danymi z wyciągu bankowego.
- Zakres: import pliku CAMT.053, parsowanie transakcji, próba automatycznego dopasowania do należności,
  pozostawienie niedopasowanych wpłat do ręcznej rekoncyliacji.
- Charakter: może być realizowane jako moduł wewnętrzny `document_api` (brak odrębnego serwisu), jeśli
  skala nie wymaga wydzielenia – do potwierdzenia przy implementacji.

### Wymiana dokumentów XML z podmiotami zewnętrznymi (EZD)

- Cel: odbiór/nadanie komunikatów sprawy w formacie XML zgodnym z zdefiniowanym schematem XSD, zgodnie z
  zasadami wymiany dokumentacji między systemami klasy EZD (np. przekazanie sprawy/dokumentu między
  jednostkami).
- Zakres: walidacja XSD, mapowanie na model sprawy, zapis dokumentu źródłowego w DMS.
- Charakter: plik wymieniany synchronicznie (upload) lub asynchronicznie (odbiór z systemu zewnętrznego) –
  w warstwie demonstracyjnej realizowane jako punkt wejścia REST w `document_api` przyjmujący XML.

### Wymiana dokumentów UBL/PEPPOL z kontrahentami (EDI)

- Cel: odbiór/nadanie dokumentów kosztowych (faktur) w formacie UBL 2.1, zgodnie z modelem sieci PEPPOL,
  jako standard elektronicznej wymiany dokumentów handlowych (EDI) zasilający moduł rozliczania płatności.
- Zakres: walidacja struktury UBL, mapowanie pozycji faktury na należność/zobowiązanie w module płatności,
  zapis dokumentu źródłowego w DMS.
- Charakter: punkt wejścia REST w `document_api` przyjmujący plik UBL; brak integracji z realnym Access
  Pointem sieci PEPPOL (zmockowane na potrzeby demonstracyjne — patrz sekcja 2).

### document_api ↔ RabbitMQ (akcje zbiorowe i zdarzenia asynchroniczne)

- Cel: wykonywanie akcji zbiorowych (np. masowa zmiana stanu wielu spraw, masowa wysyłka pism, masowe
  generowanie dokumentów z szablonu) bez blokowania żądania użytkownika, oraz propagacja zdarzeń
  asynchronicznych między serwisami (zmiana statusu przesyłki z `delivery_mock`, zob. sekcja
  `delivery_mock` poniżej).
- Zakres: dla akcji zbiorowych `document_api` publikuje komunikaty na kolejkę dedykowaną danemu
  typowi akcji; workery (w ramach `document_api` lub osobnego modułu) konsumują komunikaty i
  wykonują pojedyncze akcje, raportując postęp/wynik zbiorczy. Dla zmiany statusu przesyłki
  `document_api` jest konsumentem, a producentem jest `delivery_mock` - osobna, niezwiązana
  topologia (różne kolejki/wymiany/routing keys).
- Charakter: AMQP 0-9-1 (RabbitMQ), asynchroniczne, z potwierdzeniami (ack) i kolejkami DLX dla
  wiadomości nieprzetworzonych.

## 4. Serwisy i usługi

Architektura: jedna baza danych PostgreSQL w kontenerze, współdzielona przez wszystkie serwisy – separacja
logiczna wyłącznie za pomocą schem (jedna schema per serwis potrzebujący trwałego stanu). Każdy serwis
łączy się własną rolą bazodanową ograniczoną do swojej schemy. Pliki binarne nie są przechowywane w
PostgreSQL — każdy serwis przechowujący pliki zapisuje je na dedykowanym, wyłącznie dla niego
przypisanym wolumenie Docker (patrz sekcja „Wolumeny plikowe” poniżej).

| Schema     | Właściciel    | Zawartość                                                                                                     |
| ---------- | ------------- | ------------------------------------------------------------------------------------------------------------- |
| `core`     | document_api  | sprawy, definicje obiegów, dokumenty (metadane), strony, należności/wpłaty, użytkownicy                       |
| `dms`      | dms_service   | metadane szablonów Jasper i wersji dokumentów (treść pliku na wolumenie `dms_data`)                           |
| `delivery` | delivery_mock | przesyłki, statusy doręczeń, adresy ADE (dane mockowe)                                                        |
| `pki`      | sign_service  | certyfikaty demonstracyjne, wystawione podpisy, rejestr unieważnień                                           |
| `keycloak` | keycloak      | dane realmu Keycloak (użytkownicy, klienci, role) — zarządzane przez Keycloak, nie przez projektowy Liquibase |

Każdy serwis stanowy zarządza migracjami własnej schemy samodzielnie, przez wbudowany starter
`spring-boot-starter-liquibase` uruchamiany przy starcie aplikacji (tabele `DATABASECHANGELOG`/
`DATABASECHANGELOGLOCK` powstają w schemie danego serwisu) — nie istnieje jeden wspólny, jednorazowy
kontener Liquibase dla całego projektu.

### document_api (już zainicjalizowany)

- Rola: główny moduł domenowy – sprawy, definicje obiegów i ich stanów, strony, dekretacja, należności/
  wpłaty, orkiestracja wywołań do DMS/Jasper/podpisu/dostawców wysyłki, publikacja akcji zbiorowych do
  RabbitMQ.
- Technologia: Java 25, Spring Boot 4.1.1 (Spring Web, Spring Data JPA, Spring Validation, Spring AMQP),
  PostgreSQL (sterownik w wersji zarządzanej przez Spring Boot BOM), OAuth2 Resource Server (walidacja
  tokenów Keycloak).
- Baza danych: schema `core`.
- Port: 10620.
- UI: brak własnego (konsumowany przez `document_web`).
- CORS: skonfigurowany w `SecurityConfig` (filter chain, nie `WebMvcConfigurer`, żeby preflight
  `OPTIONS` dostał nagłówki CORS przed odrzuceniem przez `oauth2ResourceServer` jako
  nieuwierzytelniony) — originy z `cors.allowed-origins` (`DOCUMENT_WEB_ALLOWED_ORIGINS` w
  compose.yml, domyślnie `http://localhost:4200` i `http://localhost:10600`), bez credentials
  (sesja oparta na bearer tokenie, nie cookie).

### document_web (już zainicjalizowany)

- Rola: frontend główny – obsługa spraw, dokumentów, podpisów, wysyłki pism i płatności dla użytkownika.
- Technologia: Angular 21.2, PrimeNG 21.1 (open-source, motyw Aura przez `@primeuix/themes`).
  - PrimeNG jest w wersji 21 (ostatnia pełna wersja open-source przed zmianą modelu licencjonowania) —
    wymaga Angular ^21 (`peerDependencies`: `@angular/core@^21.0.7`), stąd Angular w `document_web` jest
    obniżony z 22.2 do 21.2.
  - Logowanie przez Keycloak (OIDC Authorization Code + PKCE, biblioteka `angular-oauth2-oidc`
    przypięta do `21.0.3` dla zgodności z Angular 21; realm `document-workflow`, klient `document-web`
    — konfiguracja w `deploy/compose.yml`, sekcja `keycloak` poniżej). `AuthService` opakowuje
    `OAuthService` z tej biblioteki: `zaloguj()` przekierowuje do strony logowania Keycloaka
    (`initCodeFlow()`, brak formularza login/hasło — klient ma wyłączone `directAccessGrantsEnabled`),
    `wyloguj()` woła `logOut()`, a sygnał `uzytkownik` jest zasilany z claimów id_tokenu
    (`sub`/`preferred_username`/`given_name`/`family_name`/`email`) i ról z `realm_access.roles`
    access tokenu, przefiltrowanych do pięciu znanych wartości `RolaUzytkownika` (Keycloak dorzuca też
    `default-roles-document-workflow`, `offline_access`, `uma_authorization`, które nie są rolami
    domenowymi). Pola `wydzial`/`stanowisko` nie mają odpowiednika w tokenie Keycloak — ustawiane na
    `''` do czasu dodania innego źródła tych danych. Konfiguracja OIDC (`issuer`, `clientId`,
    `documentApiBaseUrl`) jest wpiekana w bundle w czasie builda przez `src/environments/environment*.ts`
    (brak Dockerfile/runtime-config dla `document_web`).
    `requireHttps: false` explicite — wdrożenie jest na czystym HTTP (`sslRequired: none` w realmie).
    Odświeżanie sesji: `setupAutomaticSilentRefresh()` na refresh tokenie (nie iframe/silent-refresh.html).
  - Wywołania HTTP do `document_api`: bearer token dołączany automatycznie przez resourceServer
    `angular-oauth2-oidc` (`provideOAuthClient({ resourceServer: { allowedUrls, sendAccessToken: true } })`
    w `app.config.ts`) — biblioteka rejestruje swój interceptor przez klasyczny token DI
    `HTTP_INTERCEPTORS`, więc `provideHttpClient(withInterceptorsFromDi())` jest wymagane (bez tego
    Angular standalone po cichu ignorowałby interceptor i żadne żądanie nie dostałoby nagłówka
    Authorization). Zweryfikowane testem z `HttpTestingController` (`app.config.spec.ts`).
  - Moduł Sprawy jest pierwszym podłączonym do realnego `document_api` (`core/api/sprawy.api.ts` +
    `features/sprawy/sprawy.store.ts`) — wzorzec (typowany serwis API zwracający Observable +
    store na sygnałach, HTTP tylko w API, konwersja błędów przez `komunikatBleduHttp`, zdefiniowany
    w `core/api/http-error.ts` jako współdzielony między modułami) do powielenia przez kolejne
    moduły. Moduł Obywatele jest w pełni podłączony: wyszukiwarka i karta obywatela (zakładka
    "Dane podstawowe") wołają `core/api/obywatele.api.ts` przez `ObywateleDaneService`, zakładka
    "Sprawy" karty obywatela woła `core/api/sprawy.api.ts` (`SprawyApi`) bezpośrednio, zakładka
    "Korespondencja" woła `core/api/przesylki.api.ts` (`PrzesylkiApi`) bezpośrednio, zakładka
    "Rozrachunki" woła `core/api/platnosci.api.ts` (`PlatnosciApi`) bezpośrednio, a zakładka
    "Dokumenty" woła `core/api/dokumenty.api.ts` (`DokumentyApi`) bezpośrednio — wszystkie cztery
    tym samym wzorcem, z pominięciem `ObywateleDaneService` (ta klasa ma już tylko `szukaj`/`znajdz`).
    Moduł Korespondencja (`features/korespondencja/`) jest w pełni podłączony: rejestr
    (`korespondencja-strona`) i formularz nadania pisma (`korespondencja-wyslij`) wołają
    `KorespondencjaStore`/`PrzesylkiApi`, a szczegóły przesyłki (`korespondencja-szczegoly`) mają
    własny reaktywny pipeline (analogicznie do `SprawaSzczegoly`) z pollingiem `GET
/api/przesylki/:id` co 8s, bo status zmienia się asynchronicznie przez zdarzenie RabbitMQ z
    `delivery_mock` — nie ma REST endpointu do wyzwolenia/zasubskrybowania zmiany. Przycisk
    "Symuluj aktualizację statusu" (lokalny automat stanów) został usunięty na rzecz tego pollingu.
    `znakSprawy`/nazwa obywatela w rejestrze korespondencji są rozwiązywane per-distinct-id przez
    dodatkowe wywołania `SprawyApi.getById`/`ObywateleApi.getById` (Przesylka nie ma
    zdenormalizowanych nazw) - akceptowalny tradeoff przy skali danych demo.
    Moduł Płatności (`features/platnosci/`) jest w pełni podłączony: rejestr należności
    (`platnosci-strona`) woła `PlatnosciStore`/`PlatnosciApi` filtrowane po stronie backendu po
    statusie i renderuje `znakSprawy`/`obywatelNazwa` wprost z `NaleznoscListElement` (backend już
    zwraca te pola zdenormalizowane dla listy, inaczej niż `Przesylka`); szczegóły należności
    (`naleznosc-szczegoly`) mają własny reaktywny pipeline (analogicznie do `SprawaSzczegoly`/
    `KorespondencjaSzczegoly`, bez pollingu — status zmienia się tylko synchronicznie przez akcje
    użytkownika) i rozwiązują `sprawaId`/`obywatelId` (plain `Naleznosc` z `GET
/api/naleznosci/:id` nie ma zdenormalizowanych nazw) dwoma dodatkowymi wywołaniami
    `SprawyApi.getById`/`ObywateleApi.getById`; rejestr wpłat (`wplaty-lista`) woła
    `PlatnosciStore.szukajWplat`, a lista "należności nierozliczone" dla dialogu ręcznego
    dopasowania jest jednym nieprzefiltrowanym `PlatnosciApi.naleznosci({})` + filtrem po stronie
    klienta (backend filtruje po jednej wartości status, nie po OR; ta sama lista służy też do
    rozwiązania tytułu należności już w pełni rozliczonych); import CAMT.053 (`import-wyciagu`)
    wysyła realny plik przez `PlatnosciStore.importujWyciag` → `PlatnosciApi.importujCamt053`
    (multipart, pole `plik`), backend realnie parsuje XML. `POST /api/naleznosci/:id/wezwanie` nie
    istnieje — `document_api` konsumuje `jasper_service` w akcji zbiorowej "generowanie
    dokumentów", ale nie ma jeszcze dedykowanego endpointu wezwania — dialog wezwania do
    zapłaty pozostaje świadomym mockiem.
    Pozostałe moduły (Pulpit, Dokumenty, Wymiana, Administracja) są wciąż na danych mockowych
    (`core/mock/mock-data.ts`).
- Port: 10600.

### dms_service (już zainicjalizowany)

- Rola: Document Management System – przechowywanie plików (treść dokumentów i szablonów Jasper),
  wersjonowanie, rozróżnienie wersji podpisanej/niepodpisanej, metadane wersji.
- Technologia: Java 25, Spring Boot (wersja zgodna z `document_api`), Spring Web, Spring Data JPA.
- Baza danych: schema `dms` (tylko metadane).
- Przechowywanie plików: wolumen Docker `dms-data`, zamontowany w kontenerze `dms-service` pod dedykowaną
  ścieżką (`/data/dms`) — żaden inny serwis nie czyta/zapisuje plików tam poza swoim katalogiem. Jedyny
  wyjątek: jednorazowy serwis `dms-data-init` (`deploy/compose.yml`) montuje ten sam wolumen tylko po to,
  by ustawić jego własność na UID 10001 (`chown`) przed startem `dms-service` — Docker tworzy punkt
  montowania nowego wolumenu jako `root:root`, a `dms-service` działa jako nieuprzywilejowany `USER 10001`.
- Port: 10621.
- UI: brak własnego (operacje wywoływane z `document_api`; ewentualny podgląd plików przez `document_web`).

### jasper_service (już zainicjalizowany)

- Rola: silnik renderujący dokumenty na podstawie szablonów `.jrxml` i danych wejściowych (JSON) do PDF.
  Bezstanowy – nie przechowuje danych, korzysta z plików szablonów dostarczonych przez `dms_service`.
- Technologia: Java 25, Spring Boot, biblioteka JasperReports 7.0.3 (`jasperreports`, `jasperreports-pdf`,
  `jasperreports-fonts`; nie 7.1.0 — ta wersja `jasperreports-pdf` wymaga forka `openpdf` dostępnego tylko
  w repozytorium Jaspersoft, niemożliwego do rozwiązania przez lokalny mirror/Maven Central; weryfikacja
  zgodności z Jaspersoft Studio w wersji 7.x przy tworzeniu szablonów — format `.jrxml` oraz podział na
  moduły zmienił się w serii 7.x względem 6.x).
- Baza danych: brak (serwis bezstanowy).
- Port: 10622.
- UI: brak.
- Format `.jrxml` dla JasperReports 7.x: loader oparty na Jackson (XML → `JasperDesign`), NIE stary
  schemat Digester/XSD z JasperReports 5/6. Konsekwencje dla szablonów wgrywanych do `dms_service`:
  - Element `<jasperReport>` NIE może mieć atrybutu `xmlns` (namespace) — loader Jackson wykrywa
    plik po nazwie elementu głównego `jasperReport` z namespace URI == null/puste; z namespace plik
    jest ignorowany i zwracany jest ogólny błąd "Unable to load report" bez przyczyny.
  - Band-y (`title`, `pageHeader`, `detail` itd.) mają atrybuty (np. `height`) bezpośrednio na swoim
    elemencie, bez zagnieżdżonego `<band>`.
  - Elementy wizualne (`staticText`, `textField`, ...) są zagnieżdżone w `<element kind="...">` z
    atrybutami pozycji/wymiarów na `<element>`, bez `<reportElement>`. `textField` używa
    `<expression>` (nie `<textFieldExpression>`).
  - Przykład minimalnego poprawnego szablonu: `apps/jasper_service/src/test/resources/testowy-szablon.jrxml`.
  - Szablony w starym formacie (namespace + `<band>` + `<reportElement>` + `<textFieldExpression>`)
    zwrócą `400 Bad Request` przy renderowaniu — trzeba je przekonwertować do formatu JasperReports
    7.x przed wgraniem do `dms_service`.

### sign_service (już zainicjalizowany)

- Rola: składanie i weryfikacja podpisu elektronicznego (PAdES dla PDF, XAdES dla XML), znakowanie czasem,
  weryfikacja statusu certyfikatu. Demonstracyjna infrastruktura PKI (własne CA root/issuing, mock TSA
  zgodny z RFC 3161, własny OCSP/CRL responder HTTP — zob. dalej) – funkcjonalnie zbliżona do realnego
  podpisu kwalifikowanego, ale nie prawnie wiążąca.
- Technologia: Java 25, Spring Boot, biblioteka EU DSS (eu.europa.ec.joinup.sd-dss) w wersji 6.5.1,
  importowana przez `dss-bom` (moduły: `dss-xades`, `dss-pades-pdfbox`, `dss-utils-apache-commons`,
  `dss-crl-parser-x509crl`, `dss-token`, `dss-validation` - wymagany explicite, bo `dss-pades`
  deklaruje go jako `<optional>`, `dss-cms-object` - implementacja CMS wymagana w runtime przez
  ServiceLoader przy podpisywaniu PAdES/CAdES), Bouncy Castle jako prowider kryptograficzny
  (`bcpkix-jdk18on` dodany explicite do generowania root/issuing-CA i certyfikatów leaf, wersja
  zgodna z już obecnym przechodnio `bcprov-jdk18on` - 1.86, zarządzana przez `dss-bom`).
- Baza danych: schema `pki` (certyfikaty demo wraz z materiałem kryptograficznym - certyfikat DER +
  PKCS12 keystore w kolumnach `certyfikat.certyfikat_der`/`keystore_pkcs12`, generowanym programowo
  przy starcie przez idempotentny inicjalizator, nie przez Liquibase insert; tabela `demo_ca` z
  materiałem root-CA (self-signed, trust anchor) i tabela `demo_issuing_ca` z materiałem issuing-CA
  (podpisany przez root, wydaje certyfikaty leaf/TSA) - dwupoziomowa hierarchia PKI; tabela
  `demo_tsa` z materiałem mock-TSA (certyfikat z ExtendedKeyUsage id-kp-timeStamping, podpisany
  przez issuing-CA) używanym do znakowania czasem podpisów PAdES BASELINE-T; rejestr wystawionych
  podpisów; rejestr unieważnień `uniewaznienie` — serwerowe zaplecze danych dla respondera
  OCSP/CRL opisanego poniżej, nie odpytywane bezpośrednio przez logikę weryfikacji podpisu).
- Responder OCSP/CRL (pakiet `robert_neat.sign_service.revocation`): issuing-CA podpisuje
  bezpośrednio odpowiedzi OCSP (`POST /api/ocsp`, RFC 6960, BouncyCastle `OCSPReq`/`OCSPResp`) i
  wystawia CRL (`GET /api/crl/issuing-ca.crl`, X.509v2 CRL) dla certyfikatów leaf, które wydała —
  oba endpointy czytają tabelę `uniewaznienie` jako jedyne źródło prawdy o unieważnieniu i są
  bez uwierzytelnienia (konsystentnie z resztą serwisu). Certyfikaty leaf (nie root/issuing-CA/TSA)
  mają rozszerzenia `AuthorityInfoAccess` (OCSP) i `CRLDistributionPoints` wskazujące na te
  endpointy, zapisane w DER w momencie generowania (adres bazowy z
  `sign.pki.public-base-url` — zmiana tej właściwości nie aktualizuje już wygenerowanych
  certyfikatów retroaktywnie, wymaga regeneracji materiału leaf). Weryfikacja podpisu
  (`WeryfikacjaService`) odczytuje adres OCSP/CDP z samego certyfikatu podpisującego, odpytuje
  najpierw OCSP, a jeśli zawiedzie (sieć/parsowanie/niewłaściwy podpis/`unknown`) — CRL jako
  fallback; obie odpowiedzi muszą przejść kryptograficzną weryfikację podpisu kluczem publicznym
  issuing-CA, inaczej są traktowane jako nieautentyczne. Jeśli OBA kanały zawiodą, wynik jest
  fail-closed: status podpisu to `niepoprawny` — logika weryfikacji nie korzysta z tabeli
  `uniewaznienie` bezpośrednio, tylko za pośrednictwem respondera OCSP/CRL opisanego wyżej.
- Port: 10623.
- UI: brak własnego — zarządzanie certyfikatami demo ewentualnie przez prosty panel w `document_web` lub
  Thymeleaf, do ustalenia przy implementacji.

### delivery_mock (już zainicjalizowany)

- Rola: serwis mockowy symulujący dostawcę wysyłki pism — e-Doręczenia (adres ADE, dowody wysłania/
  otrzymania na wzór ADE/BAE) oraz operatora pocztowego (przesyłka hybrydowa, ZPO). Udostępnia też UI do
  ręcznej symulacji zmiany statusu przesyłki (np. „doręczono”, „zwrot”) na potrzeby demo.
- Technologia: Java 25, Spring Boot, Thymeleaf (zintegrowane UI zarządzające bez osobnej aplikacji
  frontendowej), Spring Data JPA.
- Baza danych: schema `delivery`.
- Port: 10624.
- UI: Thymeleaf, wbudowane w ten sam serwis — podgląd i symulacja statusów przesyłek.
- Publikuje zdarzenie zmiany statusu przesyłki na RabbitMQ (kolejka `przesylki-status.queue`),
  konsumowane przez `document_api`. Rejestracja client-credentials Keycloak (`delivery-mock-service`)
  jest skonfigurowana, ale obecnie nieużywana - zostawiona w razie przyszłej potrzeby wywołania
  `document_api` z uwierzytelnieniem.

### keycloak (już skonfigurowany w `deploy/compose.yml`)

- Rola: centralne uwierzytelnianie i autoryzacja (OIDC/OAuth2) dla `document_web` i API serwisów
  (`document_api` jako resource server; token Keycloak przyjmowany też przez pozostałe serwisy, jeśli
  wywoływane spoza zaufanej sieci wewnętrznej).
- Technologia: gotowy obraz `quay.io/keycloak/keycloak:26.8`, pobierany bezpośrednio z Quay (ZOT nie
  synchronizuje jeszcze tego repozytorium) — **nie wymaga pisania własnego projektu**. Konfiguracja realmu
  (`document-workflow`), klienta OIDC `document-web` (publiczny, Authorization Code + PKCE S256,
  atrybut `post.logout.redirect.uris: "+"` - pozwala na wylogowanie z przekierowaniem do tych samych
  adresów co `redirectUris`),
  klienta serwisowego `delivery-mock-service` (confidential, client-credentials — konto serwisowe
  `service-account-delivery-mock-service` z rolą realmu `kancelaria`, skonfigurowane dla
  `delivery_mock`, obecnie nieużywane: integracja z `document_api` jest asynchroniczna przez
  RabbitMQ, bez uwierzytelniania JWT), ról realmu (`administrator`, `kancelaria`, `referent`, `kierownik`,
  `ksiegowosc`) i czterech użytkowników demo (`demo.administrator`, `demo.referent`,
  `demo.kierownik`, `demo.ksiegowosc`, hasło w `KEYCLOAK_DEMO_USER_PASSWORD`) odbywa się przez
  realm-import (JSON) wpisany bezpośrednio w
  `deploy/compose.yml` jako `configs: keycloak-realm` (nie jako plik w `infra/` — na produkcję trafia
  tylko `compose.yml`, `config.env` i `projects.env`) i montowany pod `/opt/keycloak/data/import/`,
  wczytywany przy starcie (`start --import-realm`). `--import-realm` pomija realm, jeśli już istnieje w
  bazie — zmiana tego pliku po pierwszym starcie wymaga usunięcia realmu (lub wolumenu Postgresa) przed
  ponownym importem. Zweryfikowane lokalnie: realm, klienci i użytkownicy importują się poprawnie,
  endpoint JWKS i OIDC discovery odpowiadają; konto serwisowe `delivery-mock-service` (client-credentials)
  faktycznie otrzymuje token z `realm_access.roles: ["kancelaria"]`.
  `document_api` nie rejestruje własnego klienta OIDC — korzysta z Keycloaka tylko jako resource server.
- Walidacja tokenów w `document_api`: `spring.security.oauth2.resourceserver.jwt.issuer-uri` (realm
  Keycloaka bez `/protocol/openid-connect/certs`). Spring Security dociąga stamtąd
  `.well-known/openid-configuration` (w tym `jwks_uri`) leniwie — przy pierwszym żądaniu z tokenem, nie
  przy starcie aplikacji, więc `document_api` wstaje poprawnie nawet gdy Keycloak akurat nie działa.
  W `deploy/compose.yml` adres publiczny Keycloaka (`KC_HOSTNAME`/`KEYCLOAK_PUBLIC_URL`, domyślnie
  `http://localhost:10681`) różni się od adresu w sieci wewnętrznej (`http://keycloak:8080`), a to
  adres publiczny trafia do claimu `iss` w wydanych tokenach — dlatego kontener `document-api` ma
  ustawione rozdzielnie `KEYCLOAK_ISSUER_URI` (walidacja `iss`, adres publiczny) i
  `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` (pobieranie kluczy JWKS, adres wewnętrzny) —
  Spring Security pozwala ustawić obie właściwości naraz: `jwk-set-uri` decyduje o pobieraniu kluczy,
  `issuer-uri` dodatkowo włącza walidację wydawcy (`JwtIssuerValidator`).
  Mapowanie ról z `realm_access.roles` tokenu JWT na uprawnienia Spring Security (`ROLE_<rola>`) jest
  już zaimplementowane w `KeycloakRealmRoleConverter`/`SecurityConfig`.
- Baza danych: schema `keycloak` (tryb `db=postgres`, Keycloak zarządza własnymi migracjami wewnętrznie,
  niezależnie od projektowego Liquibase).
- Port: 10681 (HTTP, konsola administracyjna + endpointy OIDC), zarówno w produkcji jak i lokalnie.
- UI: wbudowana konsola administracyjna Keycloak (bez dodatkowej aplikacji).

### rabbitmq (już skonfigurowany w `deploy/compose.yml`)

- Rola: broker komunikatów dla akcji zbiorowych (masowe operacje na sprawach/dokumentach/wysyłkach) oraz
  zdarzeń asynchronicznych między serwisami.
- Technologia: obraz `rabbitmq:4-management`, pobierany bezpośrednio z Docker Hub (nie przez ZOT — mirror
  pull-through dla `library/**` z Docker Hub mógłby go też obsłużyć, ale na razie ściągamy bezpośrednio),
  konsumowany przez `document_api` przez Spring AMQP (`spring-boot-starter-amqp`).
- Baza danych: brak (RabbitMQ przechowuje stan kolejek we własnym katalogu danych na wolumenie
  `rabbitmq-data`, nie w PostgreSQL).
- Port: 10670 (AMQP), 10671 (wbudowany UI zarządzający — RabbitMQ Management Plugin), tylko lokalnie
  (`compose.dev.yml`) — w produkcji broker zostaje w sieci wewnętrznej bez portu na hoście.
- UI: wbudowany RabbitMQ Management Plugin — bez dodatkowej aplikacji.

### Wolumeny plikowe (object storage jako wolumeny Docker)

- Zasada: każdy serwis przechowujący pliki ma **własny, dedykowany wolumen** montowany wyłącznie w jego
  kontenerze, pod jedną, ustaloną ścieżką wewnątrz kontenera. Żaden inny serwis nie montuje cudzego
  wolumenu — brak współdzielonego katalogu plików między serwisami.
- `dms-data` → `dms-service` (treść dokumentów i szablonów Jasper).
- `rabbitmq-data` → `rabbitmq` (dane kolejek, persystencja wiadomości).
- `postgres-data` → kontener PostgreSQL (dane bazy — już wymagane niezależnie od tej zasady).
- Kolejne wolumeny dodawane analogicznie, jeśli kolejny serwis zacznie przechowywać pliki.

### Baza danych PostgreSQL

- Jeden kontener PostgreSQL (`192.168.1.162:5000/library/postgres:17-alpine`, przez pull-through ZOT) dla
  całego projektu, schemy: `core`, `dms`, `delivery`, `pki` (plus `keycloak` po dodaniu tego kontenera).
- Osobna rola bazodanowa per schema, z uprawnieniami ograniczonymi do własnej schemy — role i schemy
  tworzone przez skrypt inicjalizujący Postgresa (`/docker-entrypoint-initdb.d`), ponieważ Liquibase nie
  może utworzyć własnej schemy przed pierwszym połączeniem.

### Liquibase — migracje i seedowanie

- Rola: zarządzanie wersjami schematu bazy danych oraz seedowanie danych demonstracyjnych (słowniki JRWA,
  definicje obiegów, użytkownicy testowi, przykładowe sprawy).
- Model uruchomienia: **brak wspólnego, jednorazowego kontenera Liquibase** — każdy serwis stanowy
  (`document_api`, `dms_service`, `sign_service`, `delivery_mock`) zarządza migracjami własnej schemy
  samodzielnie, przez `spring-boot-starter-liquibase` uruchamiany przy starcie aplikacji
  (`spring.liquibase.change-log` + `spring.liquibase.default-schema`).
- Wersja: zarządzana przez Spring Boot BOM (Boot 4.1.1).
- Dane seedowe (kontekst `demo`) dodawane jako kolejne changesety w changelogu danego serwisu — do
  zaimplementowania w kolejnych krokach prac.

### Workflow obiegów spraw

- Wiele niezależnych **definicji obiegu** (konfiguracja: stany, dozwolone przejścia, wymagane
  dokumenty/role na danym etapie), przechowywanych jako dane konfiguracyjne w schemie `core` — nowy
  rodzaj sprawy to nowa definicja obiegu, nie zmiana kodu.
- Maszyna stanów interpretująca definicję obiegu zaimplementowana wewnątrz `document_api` (bez
  dodatkowego silnika workflow na starcie projektu).
- Rozszerzenie o dedykowany silnik (np. Flowable, we własnej schemie) możliwe w przyszłości, jeśli
  złożoność przepływów (równoległe akceptacje, SLA) przerośnie model definicji obiegu + maszyna stanów —
  do decyzji użytkownika przed wdrożeniem.
