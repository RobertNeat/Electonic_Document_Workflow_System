# Kontrakt: document_api ↔ frontend (moduł Obywatele)

Moduł Obywatele (`apps/document_web/src/app/features/obywatele/`) łączy wywołania
realnego `document_api` z danymi mockowymi (`core/mock/mock-data.ts`). `szukaj`/`znajdz`
na `ObywateleDaneService` (`obywatele-dane.service.ts`) wołają HTTP przez `ObywateleApi`
(`core/api/obywatele.api.ts`). Zakładka "Sprawy" karty obywatela (`ObywatelSprawy`) woła
HTTP bezpośrednio przez `SprawyApi` (`core/api/sprawy.api.ts`), z pominięciem
`ObywateleDaneService` — ten sam endpoint, który już obsługuje moduł Sprawy. Zakładka
"Korespondencja" (`ObywatelKorespondencja`) woła analogicznie HTTP bezpośrednio przez
`PrzesylkiApi` (`core/api/przesylki.api.ts`), też z pominięciem `ObywateleDaneService`.
Pozostałe metody na `ObywateleDaneService` (`dokumenty`/`naleznosci`/`wplaty`, a także
`sprawy` — zachowana jako pomocnicza dla `dokumenty`) wciąż czytają dane mockowe.

## Potrzebne endpointy (jeszcze nie skonsumowane)

- `GET /api/dokumenty?obywatelId=<id>` — dokumenty powiązane ze sprawami obywatela
  (widok "Dokumenty"). Obecnie frontend wylicza to po stronie klienta jako złączenie
  `sprawy(obywatelId)` → `dokumenty(sprawaId)`; backend powinien udostępnić to jako
  jedno zapytanie z dołączoną nazwą typu dokumentu i znakiem sprawy, żeby uniknąć
  wielu zapytań/łączenia po stronie frontu.
- `GET /api/naleznosci?obywatelId=<id>` — należności obywatela (widok "Rozrachunki").
- `GET /api/wplaty?naleznoscId=<id>&obywatelId=<id>` — wpłaty powiązane z
  należnościami obywatela. Obecnie frontend filtruje `MOCK_WPLATY` po zbiorze
  `naleznoscId` należącym do obywatela — backend powinien udostępnić filtrowanie
  bezpośrednio po `obywatelId` (join po stronie serwera), analogicznie do należności.

## Uwagi

- Suma "do zapłaty" na widoku Rozrachunki jest liczona po stronie klienta jako
  Σ(`kwota` − `kwotaRozliczona`) dla należności obywatela. Jeśli zbiór należności
  będzie duży, warto rozważyć osobne pole zwracane przez backend (np.
  `GET /api/obywatele/:id/saldo`), żeby uniknąć paginacji po stronie klienta.
- Karta obywatela (`/obywatele/:id` i pod-trasy) pobiera obywatela jednorazowo w
  komponencie nadrzędnym (`ObywatelKarta`) i udostępnia go pod-stronom przez lokalny
  kontekst — każdy z powyższych endpointów pod-tras powinien móc być wywołany
  niezależnie od `GET /api/obywatele/:id` (bez wymogu sekwencyjnego ładowania).
