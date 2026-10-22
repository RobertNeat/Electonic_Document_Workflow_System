# Kontrakt: document_api ↔ frontend (moduł Wymiana EDI / XML)

Moduł Wymiana EDI/XML (`apps/document_web/src/app/features/wymiana/`) korzysta
obecnie wyłącznie z danych mockowych (`wymiana-mock.ts` dla historii importów/
eksportów, `core/mock/mock-data.ts` dla `MOCK_SPRAWY`/`MOCK_SZABLONY` użytych do
symulacji mapowania i akcji zbiorowych). Walidacja XSD/UBL, parsowanie plików
i uruchamianie akcji zbiorowych są w całości zasymulowane w przeglądarce
(`setTimeout`/`setInterval`), bez żadnego wywołania sieciowego. Poniżej docelowe
zapotrzebowanie na endpointy document_api.

## Potrzebne endpointy

- `POST /api/wymiana/xml` — multipart upload komunikatu sprawy w formacie XML.
  Backend waliduje strukturę wg schematu XSD, mapuje na model `Sprawa` i zapisuje
  w DMS. Odpowiedź: wynik walidacji (sukces / lista błędów XSD z nazwą elementu
  i opisem) oraz, przy sukcesie, zmapowane pola sprawy do podglądu (strona
  `import-xml`). Frontend obecnie całość mockuje w `ImportXml.onUpload` na
  podstawie nazwy pliku (plik zawierający „niepoprawn” lub bez rozszerzenia
  `.xml` symuluje błąd walidacji).
- `GET /api/wymiana/xml/historia` — historia importów/eksportów komunikatów XML
  (nazwa pliku, kierunek, data, status, liczba przetworzonych rekordów) do
  zakładki „Wymiana XML (EZD)” na stronie przeglądu (`''`). Obecnie
  `MOCK_HISTORIA_XML` w `wymiana-mock.ts`.
- `POST /api/wymiana/ubl` — multipart upload faktury kosztowej w formacie
  UBL 2.1 (profil PEPPOL BIS Billing 3.0). Backend waliduje strukturę/przestrzeń
  nazw, parsuje nagłówek i pozycje faktury, mapuje na nową `Naleznosc` w module
  Płatności (bez automatycznego zapisu — do potwierdzenia przez księgowość).
  Odpowiedź: wynik walidacji oraz sparsowane dane faktury (numer, kontrahent,
  NIP, kwota, pozycje) i numer referencyjny proponowanej należności. Frontend
  obecnie mockuje w `ImportUbl.onUpload`.
- `GET /api/wymiana/ubl/historia` — analogicznie do historii XML, dla zakładki
  „Wymiana UBL/PEPPOL”. Obecnie `MOCK_HISTORIA_UBL` w `wymiana-mock.ts`.
- `POST /api/akcje-zbiorowe/zmiana-stanu` — body `{ sprawyId: string[], nowyStan: string }`.
  Publikuje akcję zbiorowej zmiany stanu spraw do RabbitMQ. Odpowiedź: `{ akcjaId }`
  do odpytywania statusu.
- `POST /api/akcje-zbiorowe/wysylka-pism` — body `{ sprawyId: string[], kanal: KanalWysylki }`.
  Publikuje akcję masowej wysyłki pism (e-Doręczenia / operator pocztowy / e-mail)
  do RabbitMQ. Odpowiedź: `{ akcjaId }`.
- `POST /api/akcje-zbiorowe/generowanie-dokumentow` — body `{ sprawyId: string[], szablonId: string }`.
  Publikuje akcję masowego generowania dokumentów z szablonu Jasper do RabbitMQ.
  Odpowiedź: `{ akcjaId }`.
- `GET /api/akcje-zbiorowe/:akcjaId/status` — status akcji zbiorowej: `trwajaca`
  (z postępem 0-100%) / `zakonczona` (z liczbą przetworzonych pomyślnie / łącznie
  oraz, jeśli dotyczy, listą elementów zakończonych błędem). Status jest
  aktualizowany asynchronicznie przez konsumenta RabbitMQ (worker) w miarę
  przetwarzania kolejnych spraw — frontend powinien odpytywać ten endpoint
  (polling) lub, jeśli backend to zapewni, subskrybować aktualizacje przez
  WebSocket/SSE zamiast pollingu. Obecnie `AkcjeZbiorowe` symuluje postęp lokalnie
  przez `setInterval` zwiększający sygnał 0→100% w ciągu ~3s, bez żadnego
  wywołania API.

## Uwagi

- Frontend nie zapisuje realnie wyniku importu UBL do `MOCK_NALEZNOSCI` —
  informacja o mapowaniu na należność jest tylko wyświetlana (komunikat
  informacyjny), zgodnie z zakresem tego etapu prac.
- Trzy karty akcji zbiorowych na stronie `akcje-zbiorowe` współdzielą listę
  spraw z `MOCK_SPRAWY`; backend `GET /api/sprawy` (jeśli/gdy powstanie w module
  Sprawy) powinien zasilać tę samą listę zamiast mocka.
