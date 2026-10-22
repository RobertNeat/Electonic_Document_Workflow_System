## Do zrobienia (bieżąca sesja)

- brak

## Do zrobienia (kolejne sesje)

- `dms-service`: katalog `/data/dms` na wolumenie `dms-data` nie istnieje w obrazie i przy pierwszym
  starcie zostanie utworzony przez Docker jako `root`, a kontener działa jako `USER 10001` (zob.
  `spring.Dockerfile`) — pierwszy zapis pliku skończy się `EACCES`. Do rozwiązania przy implementacji
  przechowywania plików (np. init/chown przy starcie albo wcześniejsze przygotowanie katalogu), bez zmiany
  współdzielonego `spring.Dockerfile`.
- Liquibase: changesety strukturalne + seed w kontekście `demo` dla schem `dms` (metadane szablonów/wersji
  dokumentów), `pki` (certyfikaty/podpisy demo) i `delivery` (przesyłki) — `core` już ma pełny schemat i
  seed (`apps/document_api/src/main/resources/db/changelog/core/` i `.../demo/100-seed.xml`).
- `document_api`: REST/serwis/JPA dla `obywatele`/`sprawy`/`dokumenty`/`korespondencja` gotowe (encje,
  repozytoria, mapper, serwisy, kontrolery `ObywatelController`/`SprawaController`/`DokumentController`/
  `PrzesylkaController`, testy integracyjne z Testcontainers — 39 testów). `dokumenty`/`korespondencja`
  obejmują tylko metadane w `core` — bez obsługi plików (`GET .../plik`), podpisu
  (`POST .../podpisz`, `GET .../podpis`) i uploadu szablonów (`POST /api/szablony`), bo wymagają
  jeszcze nieistniejących `dms_service`/`sign_service`/`jasper_service`; aktualizacja statusu przesyłki
  zrobiona jako synchroniczny `POST /api/przesylki/{id}/status` zamiast przez RabbitMQ/`delivery_mock`
  (to zostanie dodane przy integracji z tymi serwisami). Pozostało dla kolejnych modułów: rejestr
  należności/wpłat (import CAMT.053), wymiana XML/UBL (PEPPOL), administracja, publikacja akcji
  zbiorowych do RabbitMQ (konsumenci/workery zapisujący postęp do `akcja_zbiorowa`/
  `akcja_zbiorowa_element`), oraz `GET /api/jrwa` (potrzebuje encji `PozycjaJrwa`, nie uwzględnionej
  w dotychczasowych slice'ach).
- `document_api`: mapowanie ról realmu Keycloak (`realm_access.roles` w tokenie JWT) na uprawnienia
  Spring Security przez własny `JwtAuthenticationConverter` — obecnie tylko `jwk-set-uri` do weryfikacji
  podpisu tokenu jest skonfigurowane, bez walidacji wydawcy (`issuer-uri`) ani konwersji ról.
- `dms_service`: operacje na plikach (upload/download/wersjonowanie) na wolumenie `dms-data`.
- `jasper_service`: endpoint renderowania `.jrxml` + dane JSON → PDF.
- `sign_service`: składanie/weryfikacja podpisu PAdES/XAdES (EU DSS), demonstracyjne CA/TSA/OCSP.
- `delivery_mock`: UI Thymeleaf do symulacji statusów przesyłek (e-Doręczenia, operator pocztowy) +
  integracja REST z `document_api`.
- `document_web`: UI dla wszystkich modułów (Pulpit, Sprawy, Obywatele, Dokumenty/szablony/podpisy,
  Korespondencja, Płatności, Wymiana EDI/XML, Administracja) gotowe na danych mockowych — do zrobienia:
  podłączenie pod rzeczywiste API `document_api` (kontrakty w `currentwork/contracts/document_api_*.md`)
  oraz zastąpienie mockowego `AuthService`/formularza logowania prawdziwym logowaniem przez Keycloak
  (OIDC Authorization Code + PKCE, realm `document-workflow`, klient `document-web`, użytkownicy demo:
  `demo.administrator`, `demo.referent`, `demo.kierownik`, `demo.ksiegowosc` — hasło w
  `KEYCLOAK_DEMO_USER_PASSWORD`).
- `document_web`: migracja lokalnych modeli w `core/models/` i `features/administracja/models/` na
  pakiet współdzielony `@electronic-document-workflow/document-shared-dto` (`packages/document-shared-dto`)
  — pakiet jest gotowy (patrz `currentwork/dto-spec.md`), front jeszcze z niego nie korzysta. Przy migracji
  ujednolicić `DostawcaWysylki.typ` na wartość `'email'` (pakiet) zamiast obecnego `'e-mail'` (mock frontu).
- `document_web`: model `DefinicjaObiegu` (`core/models/sprawa.model.ts`) ma tylko płaską listę stanów
  (`stany: string[]`) bez modelu przejść/ról/wymaganych dokumentów na etapie — moduł Sprawy zakłada obieg
  liniowy jako uproszczenie. Pakiet współdzielony już ma właściwy model (`DefinicjaObiegu.przejscia`,
  zob. `dto-spec.md`) — do podłączenia przy migracji na pakiet i prawdziwy `document_api`.
- `document_web`: brak operacji wsadowych (batch) w UI na obywatelach/kontach użytkowników — model DTO już
  przygotowany pod to w pakiecie współdzielonym (`akcje-zbiorowe.ts`, `TypCeluAkcjiZbiorowej` obejmuje
  `OBYWATEL`/`UZYTKOWNIK`, nie tylko `SPRAWA`) — do wykorzystania przy dodawaniu wyboru wielu
  wierszy/akcji zbiorowych w modułach Obywatele i Administracja.
- `document_web`: `provideRouter` w `app.config.ts` nie ma `withComponentInputBinding()` — parametry tras
  czytane ręcznie przez `ActivatedRoute`/`toSignal` w kilku modułach. Do rozważenia dodanie tej opcji przy
  kolejnym porządkowaniu routingu.
