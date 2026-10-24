# Kontrakt: moduł Sprawy (document_web ↔ document_api)

Wszystkie endpointy modułu Sprawy (`GET/POST /api/sprawy`, `PATCH /api/sprawy/:id/stan`,
`GET /api/sprawy/:id/metryka`, `GET /api/definicje-obiegow`, `GET /api/slowniki/jrwa`) są już
konsumowane przez `document_web` (`SprawyApi`/`SprawyStore`). Poniżej tylko rozbieżności znalezione
przy podłączaniu frontendu, jeszcze nie naprawione po stronie backendu.

## Brak parametru sortowania w `GET /api/sprawy`

Lista jest tylko paginowana (`strona`/`rozmiarStrony`), bez parametru sortowania - lista w
`sprawy-strona.ts` nie oferuje sortowania kolumn (sortowanie klient-side tylko fragmentu danych,
jednej strony wyników, nie całości, nie miałoby sensu). Do rozważenia przy przyszłym rozszerzeniu:
`sort=pole,asc|desc` analogicznie do innych list.
