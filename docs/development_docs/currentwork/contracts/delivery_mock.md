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

## Zależność w drugą stronę: delivery_mock → document_api (zdarzenie RabbitMQ, zaimplementowane po obu stronach)

`delivery_mock` publikuje zdarzenie zmiany statusu przesyłki na RabbitMQ, konsumowane przez
`document_api`. Integracja jest zaimplementowana po obu stronach (producent: `ZmianaStatusuPrzesylkiPublisher` w
`delivery_mock`; konsument: `PrzesylkaStatusuConsumer` w `document_api`) — zgodnie z zasadą
różnicowego przechowywania kontraktów, poniżej tylko krótkie podsumowanie, nie pełny opis.

- Wymiana: `przesylki-status.exchange` (direct), kolejka `przesylki-status.queue`, routing key
  `przesylki-status` — zadeklarowane identycznie w obu modułach (`RabbitMqConfig` w
  `delivery_mock`, `PrzesylkiStatusRabbitMqConfig` w `document_api`).
- Treść wiadomości: `{ "numerReferencyjny": "string", "nowyStatus": "nadane|doreczone|odebrane|zwrot|awizo" }`
  (ten sam słownik statusów po obu stronach). Brak znacznika czasu — `document_api` stempluje
  własny czas odbioru zdarzenia.
- `document_api` odnajduje `Przesylka` po `numerReferencyjny`; nieznany numer albo nierozpoznana
  wartość statusu są logowane i odrzucane bez ponownej kolejki (nie ma odpowiedzi/callbacku do
  `delivery_mock`).
- Publikacja następuje po zatwierdzeniu lokalnego zapisu statusu w `delivery_mock`; awaria brokera
  jest łapana i zdegradowana do ostrzeżenia (lokalna zmiana statusu jest i tak zapisywana — zob.
  `SymulacjaDostawyService`).
