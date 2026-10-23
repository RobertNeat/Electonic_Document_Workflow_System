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
- Zakres: upload/download dokumentu, rejestracja nowej wersji, pobranie metadanych wersji (podpisana/
  niepodpisana), upload/listowanie szablonów Jasper wraz z wersjami.
- Charakter: REST/JSON, synchroniczne wywołania inicjowane przez `document_api`; `dms_service` nie woła
  z własnej inicjatywy do `document_api`.

### document_api ↔ jasper_service

- Cel: wygenerowanie dokumentu (PDF) na podstawie szablonu i danych przekazanych z `document_api`.
- Zakres: żądanie renderowania (id/wersja szablonu + dane w JSON) → odpowiedź z wygenerowanym plikiem PDF
  lub wskazaniem lokalizacji w DMS, jeśli `jasper_service` sam zapisuje wynik przez `dms_service`.
- Charakter: REST/JSON, synchroniczne, bezstanowe po stronie `jasper_service` (silnik renderujący, nie
  przechowuje danych).

### document_api ↔ dostawcy wysyłki pism (e-Doręczenia mock, operator pocztowy)

- Cel: nadanie pisma do adresata i śledzenie statusu doręczenia.
- Zakres: nadanie przesyłki (dokument podpisany + metadane adresata) → numer referencyjny przesyłki;
  odpytywanie/callback o zmianie statusu (nadane/doręczone/odebrane/zwrot).
- Charakter: REST/JSON jako wywołanie główne; status zwrotny jako webhook (callback do `document_api`) lub
  polling – do ustalenia osobno w kontrakcie usługi mockowej.

### document_api ↔ sign_service (podpis elektroniczny)

- Cel: złożenie i weryfikacja podpisu elektronicznego na dokumencie PDF/XML.
- Zakres: żądanie podpisania (dokument + referencja do certyfikatu podpisującego) → dokument podpisany
  (PAdES/XAdES) + znacznik czasu; weryfikacja podpisu istniejącego dokumentu.
- Charakter: REST/JSON, synchroniczne. Usługa korzysta z demonstracyjnej infrastruktury PKI (CA, TSA, OCSP
  mock) opisanej w sekcji 4.

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
  asynchronicznych między serwisami (np. zmiana statusu przesyłki z `delivery_mock`).
- Zakres: `document_api` publikuje komunikaty na kolejki dedykowane dla danego typu akcji zbiorowej;
  workery (w ramach `document_api` lub osobnego modułu) konsumują komunikaty i wykonują pojedyncze akcje,
  raportując postęp/wynik zbiorczy.
- Charakter: AMQP 0-9-1 (RabbitMQ), asynchroniczne, z potwierdzeniami (ack) i kolejkami DLX dla wiadomości
  nieprzetworzonych.

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

### document_web (już zainicjalizowany)

- Rola: frontend główny – obsługa spraw, dokumentów, podpisów, wysyłki pism i płatności dla użytkownika.
- Technologia: Angular 21.2, PrimeNG 21.1 (open-source, motyw Aura przez `@primeuix/themes`).
  - PrimeNG jest w wersji 21 (ostatnia pełna wersja open-source przed zmianą modelu licencjonowania) —
    wymaga Angular ^21 (`peerDependencies`: `@angular/core@^21.0.7`), stąd Angular w `document_web` jest
    obniżony z 22.2 do 21.2.
  - Logowanie przez Keycloak (OIDC Authorization Code + PKCE, np. `angular-oauth2-oidc` lub
    `keycloak-angular` — do wyboru przy implementacji; sam kontener Keycloak jeszcze nie istnieje, patrz
    sekcja `keycloak` poniżej).
- Port: 10600.

### dms_service (już zainicjalizowany)

- Rola: Document Management System – przechowywanie plików (treść dokumentów i szablonów Jasper),
  wersjonowanie, rozróżnienie wersji podpisanej/niepodpisanej, metadane wersji.
- Technologia: Java 25, Spring Boot (wersja zgodna z `document_api`), Spring Web, Spring Data JPA.
- Baza danych: schema `dms` (tylko metadane).
- Przechowywanie plików: wolumen Docker `dms-data`, zamontowany wyłącznie w kontenerze `dms-service` pod
  dedykowaną ścieżką (`/data/dms`) — żaden inny serwis nie montuje tego wolumenu ani nie zapisuje/czyta
  plików poza swoim katalogiem.
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

### sign_service (już zainicjalizowany)

- Rola: składanie i weryfikacja podpisu elektronicznego (PAdES dla PDF, XAdES dla XML), znakowanie czasem,
  weryfikacja statusu certyfikatu. Demonstracyjna infrastruktura PKI (własne CA root/issuing, mock TSA
  zgodny z RFC 3161, mock OCSP/CRL) – funkcjonalnie zbliżona do realnego podpisu kwalifikowanego, ale nie
  prawnie wiążąca.
- Technologia: Java 25, Spring Boot, biblioteka EU DSS (eu.europa.ec.joinup.sd-dss) w wersji 6.5.1,
  importowana przez `dss-bom` (moduły: `dss-xades`, `dss-pades-pdfbox`, `dss-utils-apache-commons`,
  `dss-crl-parser-x509crl`, `dss-token`, `dss-validation` - wymagany explicite, bo `dss-pades`
  deklaruje go jako `<optional>`, `dss-cms-object` - implementacja CMS wymagana w runtime przez
  ServiceLoader przy podpisywaniu PAdES/CAdES), Bouncy Castle jako prowider kryptograficzny
  (`bcpkix-jdk18on` dodany explicite do generowania demo-CA/certyfikatów leaf, wersja zgodna z już
  obecnym przechodnio `bcprov-jdk18on` - 1.86, zarządzana przez `dss-bom`).
- Baza danych: schema `pki` (certyfikaty demo wraz z materiałem kryptograficznym - certyfikat DER +
  PKCS12 keystore w kolumnach `certyfikat.certyfikat_der`/`keystore_pkcs12`, generowanym programowo
  przy starcie przez idempotentny inicjalizator, nie przez Liquibase insert; tabela `demo_ca` z
  materiałem jednego uproszczonego demo-CA; rejestr wystawionych podpisów; rejestr unieważnień do
  lokalnej symulacji CRL - Slice 1 obejmuje tylko PAdES BASELINE-B i lokalne sprawdzenie
  unieważnienia, bez TSA/OCSP/CRL sieciowego - zob. `plan.md`).
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

### keycloak (już skonfigurowany w `deploy/compose.yml`)

- Rola: centralne uwierzytelnianie i autoryzacja (OIDC/OAuth2) dla `document_web` i API serwisów
  (`document_api` jako resource server; token Keycloak przyjmowany też przez pozostałe serwisy, jeśli
  wywoływane spoza zaufanej sieci wewnętrznej).
- Technologia: gotowy obraz `quay.io/keycloak/keycloak:26.8`, pobierany bezpośrednio z Quay (ZOT nie
  synchronizuje jeszcze tego repozytorium) — **nie wymaga pisania własnego projektu**. Konfiguracja realmu
  (`document-workflow`), klienta OIDC `document-web` (publiczny, Authorization Code + PKCE S256), ról
  realmu (`administrator`, `kancelaria`, `referent`, `kierownik`, `ksiegowosc`) i czterech użytkowników
  demo (`demo.administrator`, `demo.referent`, `demo.kierownik`, `demo.ksiegowosc`, hasło w
  `KEYCLOAK_DEMO_USER_PASSWORD`) odbywa się przez realm-import (JSON) wpisany bezpośrednio w
  `deploy/compose.yml` jako `configs: keycloak-realm` (nie jako plik w `infra/` — na produkcję trafia
  tylko `compose.yml`, `config.env` i `projects.env`) i montowany pod `/opt/keycloak/data/import/`,
  wczytywany przy starcie (`start --import-realm`). `--import-realm` pomija realm, jeśli już istnieje w
  bazie — zmiana tego pliku po pierwszym starcie wymaga usunięcia realmu (lub wolumenu Postgresa) przed
  ponownym importem. Zweryfikowane lokalnie: realm, klient i użytkownicy importują się poprawnie, endpoint
  JWKS i OIDC discovery odpowiadają.
  `document_api` ma oddzielnego klienta nie przypisanego — korzysta z Keycloaka tylko jako resource server
  (`jwk-set-uri`), bez rejestrowania własnego klienta OIDC.
- Baza danych: schema `keycloak` (tryb `db=postgres`, Keycloak zarządza własnymi migracjami wewnętrznie,
  niezależnie od projektowego Liquibase).
- Port: 10681 (HTTP, konsola administracyjna + endpointy OIDC), zarówno w produkcji jak i lokalnie.
- UI: wbudowana konsola administracyjna Keycloak (bez dodatkowej aplikacji).
- Do zrobienia później: mapowanie ról z `realm_access.roles` tokenu JWT na uprawnienia Spring Security w
  `document_api` (obecnie tylko weryfikacja podpisu tokenu przez `jwk-set-uri`, bez konwersji ról) — zob.
  `plan.md`.
- Status: `document_api` ma już przygotowaną walidację tokenów JWT (`jwk-set-uri`) wskazującą na ten adres;
  sam kontener Keycloak (compose + realm-import) zostaje dodany w kolejnym kroku prac.

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
