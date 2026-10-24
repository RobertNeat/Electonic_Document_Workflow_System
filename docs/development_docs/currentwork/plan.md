## Do zrobienia (bieżąca sesja)

- brak

## Do zrobienia (kolejne sesje)

- `document_web`: UI dla wszystkich modułów (Pulpit, Sprawy, Obywatele, Dokumenty/szablony/podpisy,
  Korespondencja, Płatności, Wymiana EDI/XML, Administracja) gotowe na danych mockowych — do zrobienia:
  podłączenie pod rzeczywiste API `document_api` (kontrakty w `currentwork/contracts/document_api_*.md`).
  Logowanie Keycloak (OIDC Authorization Code + PKCE) jest już zrobione — zob. sekcję `document_web`
  w `main_assumptions.md`. Warunek wstępny dla rzeczywistych wywołań HTTP z przeglądarki: `document_api`
  nie ma skonfigurowanego CORS (`SecurityConfig.java`) — trzeba to dodać przed podłączeniem jakiegokolwiek
  modułu do realnego API.
