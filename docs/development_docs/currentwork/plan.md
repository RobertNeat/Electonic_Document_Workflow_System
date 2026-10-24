## Do zrobienia (bieżąca sesja)

- brak

## Do zrobienia (kolejne sesje)

- `document_web`: UI dla modułów Pulpit, Obywatele, Dokumenty/szablony/podpisy, Korespondencja,
  Płatności, Wymiana EDI/XML, Administracja gotowe na danych mockowych — do zrobienia: podłączenie
  pod rzeczywiste API `document_api` (kontrakty w `currentwork/contracts/document_api_*.md`).
  Moduł Sprawy jest już podłączony (`SprawyApi`/`SprawyStore`) — wzorzec do powielenia. CORS i
  dołączanie bearer-tokena w `document_web` (`app.config.ts`) są już gotowe dla wszystkich modułów.
- `document_api`: `SprawaService.zmienStan` nie waliduje `wymaganaRola`/`wymaganeTypyDokumentow`
  przejścia — blokowanie przycisków w `sprawa-szczegoly.ts` jest tylko UX frontendu, serwer
  przyjmie każde przejście dozwolone wg samych stanów. Szczegóły w
  `currentwork/contracts/document_api_sprawy.md`.
