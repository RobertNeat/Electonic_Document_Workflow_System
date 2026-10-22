# Kontrakt: moduł Sprawy (document_web ↔ document_api)

Frontend modułu Sprawy (`apps/document_web/src/app/features/sprawy/`) obecnie pracuje na danych
mockowych (`core/mock/mock-data.ts`) i lokalnym stanie w `SprawyStore` (sygnały, bez persystencji).
Poniżej endpointy wymagane do podłączenia modułu do `document_api`.

## GET /api/sprawy

Lista spraw z filtrowaniem i sortowaniem po stronie serwera (obecnie filtrowanie po `stan` i
`wydzial` jest robione w `computed()` na pełnej liście — do zamiany na parametry zapytania przy
większym wolumenie danych).

- Query: `stan?`, `wydzial?`, `q?` (fraza po znaku sprawy/tytule), `strona?`, `rozmiarStrony?`.
- Response: lista `Sprawa` (pola jak w `core/models/sprawa.model.ts`: id, znakSprawy, tytul, jrwa,
  definicjaObieguId, stan, obywatelId, wydzial, referent, dataWszczecia, terminUstawowy?,
  kategoriaArchiwalna, zamknieta) + metadane paginacji.

## GET /api/sprawy/:id

Szczegóły pojedynczej sprawy (`Sprawa`). 404 gdy nie istnieje.

## POST /api/sprawy

Rejestracja nowej sprawy. Request: `definicjaObieguId`, `jrwa`, `kategoriaArchiwalna`, `tytul`,
`obywatelId`, `wydzial`, `referent`, `terminUstawowy?`. Backend powinien:
- wygenerować `znakSprawy` (obecnie generowane po stronie frontendu w `SprawyStore.generujZnakSprawy`
  jako `{SYMBOL_WYDZIAŁU}.{JRWA}.{NUMER}.{ROK}` z symbolem wydziału wywnioskowanym z istniejących
  znaków — do zastąpienia właściwym słownikiem symboli wydziałów po stronie backendu),
- ustawić `stan` na `stanPoczatkowy` wybranej `DefinicjaObiegu`,
- utworzyć pierwszy wpis metryki ("Rejestracja sprawy").

Response: utworzona `Sprawa`.

## PATCH /api/sprawy/:id/stan

Zmiana stanu sprawy (obecnie: lokalna mutacja signala w `SprawyStore.zmienStan`, bez walidacji
przejść po stronie frontendu poza liniowym porządkiem `DefinicjaObiegu.stany`).

- Request: `nowyStan`.
- Backend powinien walidować, że `nowyStan` jest dozwolonym przejściem z aktualnego stanu wg
  definicji obiegu (patrz uwaga o modelu przejść poniżej), ustawić `zamknieta=true` gdy stan jest
  ostatnim w definicji, oraz dopisać wpis do metryki ("Zmiana stanu: X → Y").
- Response: zaktualizowana `Sprawa`.

## GET /api/sprawy/:id/metryka

Lista `WpisMetryki` dla sprawy, chronologicznie. Obecnie serwowane z `MOCK_METRYKI` +
wpisy dopisywane lokalnie w `SprawyStore`.

## GET /api/definicje-obiegow

Lista `DefinicjaObiegu` (id, nazwa, opis, stany, stanPoczatkowy) do wyboru w formularzu rejestracji
sprawy i do wizualizacji obiegu w szczegółach sprawy.

**Uwaga (brak w obecnym modelu, zgłoszone w raporcie agenta frontendowego):** `DefinicjaObiegu` nie
zawiera modelu przejść — frontend obecnie zakłada, że `stany` to liniowa sekwencja i każdy stan może
przejść tylko do następnego w tablicy. Zgodnie z `docs/development_docs/currentwork/plan.md`
("stany/przejścia jako konfiguracja") przy implementacji backendu należałoby dodać pole typu
`przejscia: { z: string; do: string[] }[]`, żeby wspierać gałęzie/zawracanie w obiegu (np. odesłanie
do poprawy).

## GET /api/jrwa

Lista `PozycjaJrwa` (symbol, haslo, kategoriaArchiwalna) do wyboru w formularzu rejestracji sprawy;
wybór pozycji JRWA automatycznie podpowiada kategorię archiwalną (obecnie z `MOCK_JRWA`).
