# Kontrakt: document_api ↔ frontend (moduł Obywatele)

Moduł Obywatele (`apps/document_web/src/app/features/obywatele/`) korzysta wyłącznie
z danych mockowych (`core/mock/mock-data.ts`), pośredniczonych przez lokalny
`ObywateleDaneService` (`obywatele-dane.service.ts`). To jedyne miejsce w module,
które odwołuje się do danych — podmiana na HTTP wymaga zmiany tylko tego serwisu.

## Potrzebne endpointy

- `GET /api/obywatele?query=<tekst>` — wyszukiwanie po imieniu/nazwisku, nazwie
  podmiotu, PESEL i NIP (lista/wyszukiwarka obywateli). Wyszukiwanie powinno działać
  po stronie backendu (case-insensitive, częściowe dopasowanie), żeby nie przesyłać
  całej bazy obywateli na front.
- `GET /api/obywatele/:id` — dane podstawowe obywatela (karta obywatela, widok
  "Dane podstawowe"), w tym `adresDoreczenEDE` jeśli obywatel ma aktywny adres
  e-Doręczeń.
- `GET /api/sprawy?obywatelId=<id>` — sprawy powiązane z obywatelem (widok "Sprawy").
- `GET /api/dokumenty?obywatelId=<id>` — dokumenty powiązane ze sprawami obywatela
  (widok "Dokumenty"). Obecnie frontend wylicza to po stronie klienta jako złączenie
  `sprawy(obywatelId)` → `dokumenty(sprawaId)`; backend powinien udostępnić to jako
  jedno zapytanie z dołączoną nazwą typu dokumentu i znakiem sprawy, żeby uniknąć
  wielu zapytań/łączenia po stronie frontu.
- `GET /api/przesylki?obywatelId=<id>` — korespondencja do/od obywatela (widok
  "Korespondencja"), ze statusem i kanałem wysyłki.
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
