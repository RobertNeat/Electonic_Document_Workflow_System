## Do zrobienia (bieżąca sesja)

- brak

## Do zrobienia (kolejne sesje)

- `document_web`: UI dla modułów Pulpit, Wymiana EDI/XML, Administracja gotowe na danych
  mockowych — do zrobienia: podłączenie pod rzeczywiste API `document_api` (kontrakty w
  `currentwork/contracts/document_api_*.md`). Moduły Sprawy (`SprawyApi`/`SprawyStore`),
  Korespondencja (`PrzesylkiApi`/`KorespondencjaStore`), Płatności
  (`PlatnosciApi`/`PlatnosciStore`), Obywatele i Dokumenty (`DokumentyApi`) są już podłączone —
  wzorzec do powielenia. CORS i dołączanie bearer-tokena w `document_web` (`app.config.ts`) są
  już gotowe dla wszystkich modułów.
- Moduł Dokumenty — pozostałości po podłączeniu listy/szczegółów/szablonów do rzeczywistego API
  (zob. `currentwork/contracts/document_api_dokumenty.md` dla szczegółów):
  - `GET /api/dokumenty/:id/wersje/:wersja/plik` (podgląd/pobranie PDF, proxy do `dms_service`)
    — akcje "Podgląd PDF" i "Pobierz" w `dokument-szczegoly.ts` zostają mockiem do tego czasu.
  - `GET /api/dokumenty/:id/wersje/:wersja/podpis` (szczegóły podpisu — DTO `SzczegolyPodpisu`
    już istnieje w backendzie, brak kontrolera) — dialog "Szczegóły podpisu" zostaje mockiem.
  - `POST /api/szablony` (multipart upload szablonu `.jrxml`) — dialog uploadu w
    `szablony-strona.ts` zostaje mockiem (lokalna mutacja listy, bez zapisu).
  - Podłączenie `POST /api/dokumenty/:id/podpisz` (już zaimplementowany w backendzie) —
    zablokowane na: `GET /api/certyfikaty` (proxy do `sign_service`) + decyzja produktowa,
    jakie certyfikaty użytkownik może wybrać do podpisu, + cert-picker dialog we froncie.
