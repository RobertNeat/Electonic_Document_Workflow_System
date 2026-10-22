# Kontrakt: document_api ↔ frontend (moduł Dokumenty i szablony)

Moduł Dokumenty (`apps/document_web/src/app/features/dokumenty/`) obecnie korzysta
wyłącznie z danych mockowych (`core/mock/mock-data.ts`). Poniżej docelowe zapotrzebowanie
na endpointy document_api, które będzie orkiestrować wywołania do `dms_service`
(pliki/wersje), `jasper_service` (szablony/generowanie) i `sign_service` (podpis PAdES).

## Potrzebne endpointy

- `GET /api/dokumenty?sprawaId=<id>&typDokumentuId=<id>` — lista dokumentów z
  filtrem po sprawie (query param na liście) i typie dokumentu. Odpowiedź powinna
  zawierać już rozwiązany `znakSprawy` i status podpisu najnowszej wersji, żeby
  front nie musiał robić N+1 zapytań do spraw/wersji przy renderowaniu tabeli.
- `GET /api/dokumenty/:id` — szczegóły dokumentu (nazwa, typ, sprawa).
- `GET /api/dokumenty/:id/wersje` — lista wersji dokumentu (numer, data, autor,
  `podpisana`, `statusPodpisu`), sortowana malejąco po numerze wersji.
- `GET /api/dokumenty/:id/wersje/:wersja/plik` — pobranie/podgląd pliku PDF danej
  wersji (proxy do `dms_service`); front planuje użyć tego do akcji "Podgląd PDF"
  i "Pobierz" (obecnie mock bez realnego pliku).
- `POST /api/dokumenty/:id/podpisz` — body: `{ wersja: number }`. Orkiestruje wywołanie
  `sign_service` (PAdES + TSA) i aktualizuje `statusPodpisu`/`podpisana` wersji.
  Wersja podpisana powinna stać się niemutowalna (kolejne zmiany tworzą nową wersję
  roboczą, nie nadpisują podpisanej).
- `GET /api/dokumenty/:id/wersje/:wersja/podpis` — szczegóły podpisu: podpisujący
  (CN z certyfikatu), czas podpisu, odcisk certyfikatu, status OCSP, status CRL,
  znacznik czasu TSA. Front obecnie renderuje te pola mockowo w dialogu
  "Szczegóły podpisu" — potrzebuje struktury 1:1 z tymi polami.
- `GET /api/szablony?typDokumentuId=<id>` — lista szablonów Jasper (nazwa pliku
  `.jrxml`, typ dokumentu, wersja, data wgrania, autor).
- `GET /api/szablony/:id/historia` — historia wersji szablonu (wersja, data,
  autor, opis zmiany) do dialogu "Historia wersji" na liście szablonów.
- `POST /api/szablony` — multipart upload nowego pliku `.jrxml` (+ `typDokumentuId`),
  orkiestruje zapis w `jasper_service` i wersjonowanie. Walidacja rozszerzenia
  `.jrxml` jest już robiona po stronie frontu, ale backend musi to też weryfikować.

## Uwagi

- Front liczy, że `WersjaDokumentu.statusPodpisu` ma wartość `'brak'` dla wersji
  roboczych (niepodpisanych) — to jest rozróżniane kolorystycznie (szary) od
  statusów błędu (czerwony: `niepoprawny`, `certyfikat_wygasly`,
  `certyfikat_uniewazniony`) i sukcesu (zielony: `poprawny`). Backend powinien
  zachować ten sam zestaw wartości enum.
- Link do sprawy z listy dokumentów (`/sprawy/:id`) zakłada, że `sprawaId` w
  `Dokument` odpowiada `id` zasobu `Sprawa` z modułu Sprawy — bez dodatkowego
  mapowania.
- Akcja "Podpisz" w UI jest obecnie czysto lokalna (zmienia sygnał w komponencie)
  — po podłączeniu `POST /api/dokumenty/:id/podpisz` front powinien zastąpić
  optymistyczną aktualizację odpowiedzią z backendu (żeby odzwierciedlić realny
  status z `sign_service`, który może zwrócić błąd/odrzucenie podpisu).
