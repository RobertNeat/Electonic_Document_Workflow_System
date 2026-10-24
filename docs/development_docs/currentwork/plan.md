## Do zrobienia (bieżąca sesja)

- brak

## Do zrobienia (kolejne sesje)

- `document_web`: UI dla modułów Pulpit, Dokumenty/szablony/podpisy, Wymiana EDI/XML,
  Administracja gotowe na danych mockowych — do zrobienia: podłączenie pod rzeczywiste API
  `document_api` (kontrakty w `currentwork/contracts/document_api_*.md`). Moduły Sprawy
  (`SprawyApi`/`SprawyStore`), Korespondencja (`PrzesylkiApi`/`KorespondencjaStore`) i Płatności
  (`PlatnosciApi`/`PlatnosciStore`) są już podłączone — wzorzec do powielenia. CORS i dołączanie
  bearer-tokena w `document_web` (`app.config.ts`) są już gotowe dla wszystkich modułów.
