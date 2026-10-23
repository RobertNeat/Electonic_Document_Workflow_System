# Kontrakt: dms_service → document_api (operacje na plikach)

`dms_service` (port 10621, schema Postgres `dms`) wystawia REST API do przechowywania
plików szablonów Jasper i wersji dokumentów na wolumenie `dms-data`. `document_api`
jeszcze NIE woła do tego serwisu (zob. `main_assumptions.md` sekcja
"document_api ↔ dms_service") — poniższy opis jest kontraktem wystawionym, czekającym
na konsumenta.

Brak integracji JWT/Keycloak w `dms_service` na tym etapie — pole `autor` przy
uploadzie jest przyjmowane jako parametr formularza, nie odczytywane z tokenu.

## Szablony Jasper

### `GET /api/szablony`
Lista wszystkich szablonów. Odpowiedź: `SzablonJasper[]`.

### `GET /api/szablony/{id}`
Metadane jednego szablonu. 404 (`{ "komunikat": string }`) jeśli nie istnieje.

### `POST /api/szablony`
`multipart/form-data`:
- `plik` — plik `.jrxml`.
- `typDokumentuId` (string, wymagane) — id typu dokumentu z `core.typ_dokumentu` (referencja płaska, bez FK).
- `nazwa` (string, wymagane) — nazwa logiczna szablonu.
- `autor` (string, wymagane).

Wersjonowanie: kolejny upload tej samej `(typDokumentuId, nazwa)` zwiększa `wersja`
(`MAX(wersja)+1`), nie nadpisuje poprzedniego wiersza/pliku. Odpowiedź: `201 Created`,
body `SzablonJasper`.

### `GET /api/szablony/{id}/plik`
Pobranie zawartości pliku szablonu. `Content-Type` wg rozszerzenia nazwy (fallback
`application/octet-stream`), `Content-Disposition: attachment` z oryginalną nazwą.
404 jeśli metadane lub plik na dysku nie istnieją.

### DTO `SzablonJasper`
```json
{
  "id": "string",
  "typDokumentuId": "string",
  "nazwa": "string",
  "wersja": 1,
  "dataWgrania": "2026-10-23",
  "autor": "string"
}
```
(`sciezkaPliku` jest szczegółem przechowywania — nie ujawniany w odpowiedzi.)

## Wersje dokumentów

### `GET /api/dokumenty/{dokumentId}/wersje`
Lista wersji dla danego `dokumentId` (referencja płaska do `core.dokument.id`),
sortowana po `wersja` wstępująco. Nieznany `dokumentId` → `200 OK` z `[]` (brak tabeli
`dokument` po stronie `dms_service`, więc nie ma względem czego zwrócić 404).

### `GET /api/dokumenty/{dokumentId}/wersje/{wersja}`
Metadane jednej wersji. 404 jeśli nie istnieje.

### `POST /api/dokumenty/{dokumentId}/wersje`
`multipart/form-data`:
- `plik` — plik dokumentu (rozszerzenie zachowywane z oryginalnej nazwy).
- `autor` (string, wymagane).

`wersja = MAX(wersja istniejących dla dokumentId)+1` (1 jeśli brak). Nowa wersja
zawsze tworzona jako `podpisana=false`, `statusPodpisu="brak"` — podpis cyfrowy jest
poza zakresem `dms_service` (zob. `sign_service`, nieistniejący jeszcze). Odpowiedź:
`201 Created`, body `WersjaDokumentuDms`.

### `GET /api/dokumenty/{dokumentId}/wersje/{wersja}/plik`
Pobranie zawartości pliku tej wersji. Nagłówki jak w szablonach. 404 jeśli metadane
lub plik na dysku nie istnieją.

### DTO `WersjaDokumentuDms`
```json
{
  "id": "string",
  "dokumentId": "string",
  "wersja": 1,
  "podpisana": false,
  "dataUtworzenia": "2026-10-23",
  "autor": "string",
  "statusPodpisu": "brak"
}
```
`statusPodpisu` ∈ `brak | poprawny | niepoprawny | certyfikat_wygasly | certyfikat_uniewazniony`
(sam `dms_service` ustawia tylko `brak` przy uploadzie — aktualizacja statusu po
podpisaniu to zakres przyszłej integracji z `sign_service`, poza tym kontraktem).

## Błędy

Ujednolicony format błędu (analogiczny do `document_api`):
```json
{ "komunikat": "string" }
```
- `400 Bad Request` — brak/niepoprawne pola formularza, niedozwolona nazwa pliku lub
  identyfikator (ochrona przed path traversal/zip-slip: `..`, `/`, `\`, `:` są
  odrzucane, nie czyszczone).
- `404 Not Found` — zasób metadanych lub plik na dysku nie istnieje.
