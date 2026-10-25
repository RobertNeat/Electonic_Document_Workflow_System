# Kontrakt: document_api ↔ frontend (moduł Pulpit)

Moduł Pulpit (`apps/document_web/src/app/features/pulpit/pulpit-strona/pulpit-strona.ts`)
jest podłączony do rzeczywistych endpointów (`GET /api/sprawy`, `GET /api/naleznosci`,
`GET /api/przesylki`, `GET /api/obywatele/:id`). Pozostały gap jest wyłącznie backendowy -
frontend obchodzi go filtrowaniem klientowym, patrz `pulpit-strona.ts`:

- `GET /api/sprawy` nie wspiera filtra `referent` - frontend pociąga stronę
  `rozmiarStrony=200` bez filtra i filtruje po `referent === "<imie> <nazwisko>"` lokalnie.
- `GET /api/naleznosci` i `GET /api/przesylki` nie wspierają filtra `status` jako OR
  (wielowartościowego) ani filtra `wydzial` - frontend pociąga pełne listy bez filtra
  `status` i filtruje lokalnie po zbiorze statusów oraz po wydziale (join przez `sprawaId`
  na listę spraw już pociągniętą przez Pulpit).
- Token Keycloak nie zawiera atrybutu wydziału użytkownika (`auth-claims.ts`) - Pulpit
  przybliża wydział referenta wydziałem jego najnowszej sprawy (`mojeSprawy()[0]?.wydzial`).
