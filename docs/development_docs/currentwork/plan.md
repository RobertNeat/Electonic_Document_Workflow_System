## Do zrobienia (bieżąca sesja)

- brak

## Do zrobienia (kolejne sesje)

- `dms-service`: katalog `/data/dms` na wolumenie `dms-data` nie istnieje w obrazie i przy pierwszym
  starcie zostanie utworzony przez Docker jako `root`, a kontener działa jako `USER 10001` (zob.
  `spring.Dockerfile`) — pierwszy zapis pliku skończy się `EACCES`. Do rozwiązania przy implementacji
  przechowywania plików (np. init/chown przy starcie albo wcześniejsze przygotowanie katalogu), bez zmiany
  współdzielonego `spring.Dockerfile`.
- `document_api`: wpis statusu przesyłki jest obecnie synchronicznym `POST /api/przesylki/{id}/status`
  — do zastąpienia odbiorem zdarzenia z RabbitMQ/`delivery_mock`, gdy ten serwis powstanie.
- `document_api`: dziennik zdarzeń (`wpis_dziennika_zdarzen`/`AuditLogController`) obecnie tylko
  odczytuje/filtruje istniejące wpisy — brak automatycznej emisji zdarzeń audytowych z innych
  modułów (sprawy, dokumenty, płatności) przy ich operacjach; do zrobienia przy okazji
  event-sourcingu operacji.
- `document_api`: walidacja wydawcy tokenu JWT (`issuer-uri`) nieskonfigurowana — obecnie tylko
  `jwk-set-uri` do weryfikacji podpisu.
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
