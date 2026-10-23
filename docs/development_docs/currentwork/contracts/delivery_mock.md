# Kontrakt: delivery_mock (REST API wystawione)

`delivery_mock` (port 10624, schema Postgres `delivery`) wystawia REST API do przesyłek
dostawy (stanu doręczenia widzianego z perspektywy "dostawcy" — e-Doręczenia/operator
pocztowy/email), głównie dla symetrii z resztą systemu i ewentualnych testów integracyjnych
UI/panelu Thymeleaf. Żaden inny moduł (`document_api`, `document_web`) jeszcze NIE woła do
tego API — poniższy opis jest kontraktem wystawionym, czekającym na konsumenta.

Panel symulacji statusów (UI Thymeleaf, `GET /przesylki`, `GET /przesylki/{id}`, formularz
"Symuluj zmianę statusu") jest osobną, nie-REST ścieżką (klasyczny Spring MVC z przekierowaniem
po submit) i nie jest tu opisany jako kontrakt — to wewnętrzny interfejs obsługowy serwisu.

## `GET /api/przesylki-dostawy`
Lista wszystkich przesyłek dostawy. Odpowiedź: `PrzesylkaDostawy[]`.

## `GET /api/przesylki-dostawy/{id}`
Szczegóły przesyłki wraz z chronologiczną historią statusów. 404
(`{ "komunikat": string }`) jeśli `id` nie istnieje. Odpowiedź:
```json
{
  "przesylka": { "...": "PrzesylkaDostawy" },
  "historiaStatusow": [ { "...": "WpisStatusuDostawy" } ]
}
```

## `POST /api/przesylki-dostawy`
Rejestracja nowej przesyłki do symulacji (niezależnie od seeda demo). Body:
```json
{ "numerReferencyjny": "string", "kanal": "e-doreczenia|operator_pocztowy|email", "adresAde": "string|null" }
```
Status początkowy zawsze `"nadane"`, `dataNadania`/`dataStatusu` ustawiane na `now()` przez
serwis (nie przyjmowane od klienta). `400 Bad Request` jeśli `kanal` nieznany albo
`numerReferencyjny` już istnieje (unikalność `przesylka_dostawy.numer_referencyjny`).
Odpowiedź: `201 Created`, body `PrzesylkaDostawy`.

## DTO `PrzesylkaDostawy`
```json
{
  "id": "string",
  "numerReferencyjny": "string",
  "kanal": "e-doreczenia|operator_pocztowy|email",
  "adresAde": "string|null",
  "status": "nadane|doreczone|odebrane|zwrot|awizo",
  "dataNadania": "2026-10-23T00:00:00+02:00",
  "dataStatusu": "2026-10-23T00:00:00+02:00"
}
```

## DTO `WpisStatusuDostawy`
```json
{
  "id": "string",
  "przesylkaDostawyId": "string",
  "status": "nadane|doreczone|odebrane|zwrot|awizo",
  "data": "2026-10-23T00:00:00+02:00",
  "szczegoly": "string|null"
}
```

## Błędy
Ujednolicony format błędu (analogiczny do `document_api`/`dms_service`):
```json
{ "komunikat": "string" }
```
- `400 Bad Request` — nieznany `kanal`, zduplikowany `numerReferencyjny`, błąd walidacji pól.
- `404 Not Found` — przesyłka dostawy o podanym `id` nie istnieje.

---

## Zależność w drugą stronę: delivery_mock → document_api (już zaimplementowana po obu stronach)

`delivery_mock` jest KONSUMENTEM `document_api`, nie odwrotnie — to integracja zaimplementowana
w tym zadaniu po stronie klienta (`DocumentApiClient`), wołająca już istniejące endpointy
`document_api` (`PrzesylkaController`: `GET /api/przesylki`, `POST /api/przesylki/{id}/status`
z body `AktualizujStatusPrzesylkiRequest { "nowyStatus": "<status>" }`). Skoro integracja jest
zaimplementowana po obu stronach, nie wymaga odrębnego pliku kontraktu opisującego stronę
`document_api` (zgodnie z zasadą różnicowego przechowywania kontraktów) — szczegóły wywołania
są w kodzie `SymulacjaDostawyService`/`DocumentApiClient` w `delivery_mock`.

Uwaga do weryfikacji przy wdrożeniu: `document_api` wymaga uwierzytelnienia JWT (OAuth2 Resource
Server, Keycloak) na `GET /api/przesylki` (`anyRequest().authenticated()`) i roli
`kancelaria`/`referent`/`kierownik`/`administrator` na `POST /api/przesylki/{id}/status`
(`SecurityConfig`). `delivery_mock` woła te endpointy bez żadnego tokenu — w obecnym stanie
synchronizacja zawsze zwróci `401`, co jest łapane i zdegradowane do ostrzeżenia (lokalna zmiana
statusu w `delivery_mock` jest i tak zapisywana — zob. `SymulacjaDostawyService`). Do decyzji
użytkownika w przyszłej sesji: konto serwisowe (client-credentials) dla `delivery_mock` albo
wyjątek `permitAll`/zaufana sieć wewnętrzna dla wywołań serwis-serwis.
