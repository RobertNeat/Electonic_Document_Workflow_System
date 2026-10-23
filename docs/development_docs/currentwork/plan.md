## Do zrobienia (bieżąca sesja)

- brak

## Do zrobienia (kolejne sesje)

- `dms_service`: punkt montowania wolumenu `dms-data` jest tworzony przez Docker jako `root:root`,
  mode `755`, więc kontener działający jako `USER 10001` nie ma do niego praw zapisu;
  `StorageInitializer` zatrzymuje start aplikacji czytelnym błędem w tej sytuacji, ale samej
  własności katalogu nie naprawia. Do zrobienia: init/chown wolumenu `dms-data` na poziomie
  `deploy/compose*.yml` (serwis `dms_service` nie jest tam jeszcze podłączony), bez zmiany
  współdzielonego `spring.Dockerfile`.
- `document_api`: wpis statusu przesyłki jest obecnie synchronicznym `POST /api/przesylki/{id}/status`
  — do zastąpienia odbiorem zdarzenia z RabbitMQ/`delivery_mock`, gdy ten serwis powstanie.
- `document_api`: dziennik zdarzeń (`wpis_dziennika_zdarzen`/`AuditLogController`) obecnie tylko
  odczytuje/filtruje istniejące wpisy — brak automatycznej emisji zdarzeń audytowych z innych
  modułów (sprawy, dokumenty, płatności) przy ich operacjach; do zrobienia przy okazji
  event-sourcingu operacji.
- `sign_service`: XAdES (podpis dokumentów/komunikatów XML) - nieobjęte Slice 1 (tylko PAdES
  zaimplementowany).
- `sign_service`: TSA mock zgodny z RFC 3161 - obecnie podpisy PAdES są tylko BASELINE-B (bez
  znacznika czasu), `wystawiony_podpis.znacznik_czasu` ma placeholder `"brak-tsa"`.
- `sign_service`: prawdziwy serwer OCSP/CRL (HTTP responder odpytywany przez AIA/CDP z
  certyfikatu) - obecnie weryfikacja unieważnienia jest WYŁĄCZNIE lokalna (tabela `uniewaznienie`),
  bez żadnego wywołania sieciowego; wygenerowane certyfikaty demo nie mają rozszerzeń AIA/CDP.
- `sign_service`: root CA + issuing CA jako dwa odrębne podmioty - obecnie jedno uproszczone
  demo-CA (`demo_ca`, jeden self-signed klucz/certyfikat) podpisuje certyfikaty leaf bezpośrednio.
- `sign_service` ↔ `dms_service`/`document_api`: orkiestracja pobierania dokumentu do podpisania
  z DMS - obecnie `sign_service` przyjmuje bajty PDF bezpośrednio w żądaniu, nie woła do
  `dms_service`; integracja ma być inicjowana przez `document_api` w przyszłości.
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
