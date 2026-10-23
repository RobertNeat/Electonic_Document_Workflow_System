# Kontrakt: jasper_service → document_api (renderowanie PDF)

`jasper_service` (port 10622) wystawia REST API renderujące PDF z szablonu Jasper +
danych JSON. `document_api` jeszcze NIE woła do tego serwisu (zob.
`main_assumptions.md` sekcja "document_api ↔ jasper_service") — poniższy opis jest
kontraktem wystawionym, czekającym na konsumenta.

`jasper_service` jest bezstanowy - pobiera plik szablonu `.jrxml` z `dms_service`
(`GET /api/szablony/{id}` + `GET /api/szablony/{id}/plik`) przy każdym żądaniu
renderowania, nic nie cache'uje i nie przechowuje.

## `POST /api/render/{szablonId}`

Request: `application/json`, body - dowolna płaska mapa `Map<String, Object>`
(parametry raportu, klucz→wartość; proste typy: String/Number/Boolean). Pusty
obiekt `{}` lub brak body jest akceptowany (raport może nie mieć parametrów).

Parametry z body są przekazywane do JasperReports jako `parameters` (`$P{klucz}`
w szablonie), NIE jako datasource - raport renderowany jest z `JREmptyDataSource`
(brak obsługi tabel/subdatasource na tym etapie).

Response: `200 OK`, `Content-Type: application/pdf`,
`Content-Disposition: inline; filename="{nazwa-szablonu-bez-rozszerzenia}.pdf"`,
body = bajty PDF.

### Błędy

Ujednolicony format błędu (analogiczny do `dms_service`/`document_api`):
```json
{ "komunikat": "string" }
```
- `404 Not Found` — szablon o podanym `szablonId` nie istnieje w `dms_service`.
- `400 Bad Request` — szablon `.jrxml` niepoprawny/uszkodzony, niezgodny z
  formatem JasperReports 7.x, lub dane wejściowe nie pozwalają na wypełnienie
  raportu (np. brakujący wymagany parametr powodujący błąd wyrażenia). Obejmuje
  też błędy komunikacji z `dms_service` inne niż 404 (np. serwis nieosiągalny).

## Ważne: format `.jrxml` dla JasperReports 7.x

JasperReports 7.0.3 wczytuje `.jrxml` przez loader oparty na Jackson (XML →
`JasperDesign`), NIE przez stary schemat Digester/XSD znany z JasperReports 5/6.
Konsekwencje dla szablonów wgrywanych do `dms_service`:
- Element `<jasperReport>` NIE może mieć atrybutu `xmlns` (namespace) — loader
  Jackson wykrywa plik po nazwie elementu głównego `jasperReport` z namespace
  URI == null/puste; z namespace plik jest ignorowany i zwracany jest ogólny
  błąd "Unable to load report" bez przyczyny.
- Band-y (`title`, `pageHeader`, `detail` itd.) mają atrybuty (np. `height`)
  bezpośrednio na swoim elemencie, bez zagnieżdżonego `<band>`.
- Elementy wizualne (`staticText`, `textField`, ...) są zagnieżdżone w
  `<element kind="...">` z atrybutami pozycji/wymiarów na `<element>`, bez
  `<reportElement>`. `textField` używa `<expression>` (nie
  `<textFieldExpression>`).
- Przykład minimalnego poprawnego szablonu:
  `apps/jasper_service/src/test/resources/testowy-szablon.jrxml`.

Szablony w starym formacie (namespace + `<band>` + `<reportElement>` +
`<textFieldExpression>`) zwrócą `400 Bad Request` przy renderowaniu — trzeba je
przekonwertować do formatu JasperReports 7.x przed wgraniem do `dms_service`.
