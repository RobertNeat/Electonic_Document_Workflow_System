# Kontrakt: moduł Sprawy (document_web ↔ document_api)

Wszystkie endpointy modułu Sprawy (`GET/POST /api/sprawy`, `PATCH /api/sprawy/:id/stan`,
`GET /api/sprawy/:id/metryka`, `GET /api/definicje-obiegow`, `GET /api/slowniki/jrwa`) są już
konsumowane przez `document_web` (`SprawyApi`/`SprawyStore`). Poniżej tylko rozbieżności znalezione
przy podłączaniu frontendu, jeszcze nie naprawione po stronie backendu.

## `PATCH /api/sprawy/:id/stan` nie waliduje `wymaganaRola`/`wymaganeTypyDokumentow`

`SprawaService.zmienStan` sprawdza tylko, czy `nowyStan` jest w `PrzejscieObieguEntity.doStanow`
dla aktualnego stanu - nie sprawdza `wymagana_rola` ani `wymagane_typy_dokumentow` tego przejścia.
Blokowanie przycisków przejścia w `sprawa-szczegoly.ts` (`przejsciaZeStanu`) jest więc tylko UX po
stronie frontendu: referent może PATCH-ować np. "Do akceptacji" → "Zakończona" w obiegu
`obieg-decyzja` bez bycia kierownikiem i bez wymaganego dokumentu, mimo że UI by to zablokowało.
Do naprawienia: `SprawaService.zmienStan` powinien odczytać `wymaganaRola`/`wymaganeTypyDokumentow`
znalezionego `PrzejscieObieguEntity` i rzucić `BadRequestException`/`IllegalStateTransitionException`,
gdy wołający (rola z JWT) lub istniejące dokumenty sprawy nie spełniają wymogu.

## `PrzejscieObieguRepository.findZeStanu` zwraca `Optional` - nie obsłużone rozgałęzienie

Zapytanie zwraca jeden wiersz (`Optional<PrzejscieObieguEntity>`) dla danej pary
(definicjaObieguId, zStanu). Obecny seed demo nie ma rozgałęzień (każdy stan ma dokładnie jedno
wychodzące `przejscie_obiegu`), więc to dziś nie powoduje błędu - ale gdyby ktoś dodał drugi wiersz
`przejscie_obiegu` z tym samym `z_stanu` (np. żeby odwzorować rozgałęzienie "akceptuj" / "odeślij do
poprawek", jak ma to mockowy `MOCK_DEFINICJE_OBIEGOW` na froncie), zapytanie rzuciłoby
`IncorrectResultSizeDataAccessException`, niezmapowany w `GlobalExceptionHandler` - czyli
nieobsłużony 500. Do naprawienia przy pierwszej potrzebie realnego rozgałęzienia w obiegu.

## Brak parametru sortowania w `GET /api/sprawy`

Lista jest tylko paginowana (`strona`/`rozmiarStrony`), bez parametru sortowania - lista w
`sprawy-strona.ts` nie oferuje sortowania kolumn (sortowanie klient-side tylko fragmentu danych,
jednej strony wyników, nie całości, nie miałoby sensu). Do rozważenia przy przyszłym rozszerzeniu:
`sort=pole,asc|desc` analogicznie do innych list.
