## Do zrobienia (bieżąca sesja)

- brak

## Do zrobienia (kolejne sesje)

- `document_web`: UI dla modułów Pulpit, Dokumenty/szablony/podpisy, Korespondencja, Płatności,
  Wymiana EDI/XML, Administracja gotowe na danych mockowych — do zrobienia: podłączenie pod
  rzeczywiste API `document_api` (kontrakty w `currentwork/contracts/document_api_*.md`). Moduł
  Sprawy jest już podłączony (`SprawyApi`/`SprawyStore`) — wzorzec do powielenia. CORS i
  dołączanie bearer-tokena w `document_web` (`app.config.ts`) są już gotowe dla wszystkich modułów.
- `document_web` moduł Obywatele: wyszukiwarka (`GET /api/obywatele?query=`), karta obywatela,
  zakładka "Dane podstawowe" (`GET /api/obywatele/:id`), zakładka "Sprawy" (`GET /api/sprawy?obywatelId=<id>`
  przez `SprawyApi`) i zakładka "Korespondencja" (`GET /api/przesylki?obywatelId=<id>` przez
  `PrzesylkiApi`) są już podłączone. Pozostałe dwie zakładki karty obywatela
  wciąż czytają mocki (`ObywateleDaneService.dokumenty/naleznosci/wplaty`) — do
  zrobienia, każda jako osobny, wąski cykl:
  - Zakładka Dokumenty: wymaga NOWEGO zagregowanego endpointu `GET /api/dokumenty?obywatelId=<id>`
    z dołączoną nazwą typu dokumentu i znakiem sprawy (obecnie front liczy to po stronie klienta
    jako złączenie sprawy→dokumenty) — backend nie istnieje.
  - Zakładka Rozrachunki: wymaga dodania filtra `obywatelId` do istniejących
    `GET /api/naleznosci` i `GET /api/wplaty` (dziś `wplaty` filtruje tylko po `naleznoscId`, bez
    `obywatelId`).
