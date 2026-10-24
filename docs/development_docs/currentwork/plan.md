## Do zrobienia (bieżąca sesja)

- brak

## Do zrobienia (kolejne sesje)

- `document_web`: UI dla wszystkich modułów (Pulpit, Sprawy, Obywatele, Dokumenty/szablony/podpisy,
  Korespondencja, Płatności, Wymiana EDI/XML, Administracja) gotowe na danych mockowych — do zrobienia:
  podłączenie pod rzeczywiste API `document_api` (kontrakty w `currentwork/contracts/document_api_*.md`)
  oraz zastąpienie mockowego `AuthService`/formularza logowania prawdziwym logowaniem przez Keycloak
  (OIDC Authorization Code + PKCE, realm `document-workflow`, klient `document-web`, użytkownicy demo:
  `demo.administrator`, `demo.referent`, `demo.kierownik`, `demo.ksiegowosc` — hasło w
  `KEYCLOAK_DEMO_USER_PASSWORD`).
