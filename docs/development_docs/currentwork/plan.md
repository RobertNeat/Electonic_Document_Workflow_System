## Do zrobienia (bieżąca sesja)

- brak

## Do zrobienia (kolejne sesje)

- `document_api`: wpis statusu przesyłki jest obecnie synchronicznym `POST /api/przesylki/{id}/status`
  — do zastąpienia odbiorem zdarzenia z RabbitMQ/`delivery_mock`, gdy ten serwis powstanie.
- `sign_service`: XAdES (podpis dokumentów/komunikatów XML) - nieobjęte Slice 1 (tylko PAdES
  zaimplementowany).
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
