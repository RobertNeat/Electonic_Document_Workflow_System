# Kontrakt: document_api ↔ frontend (moduł Dokumenty i szablony)

Moduł Dokumenty (`apps/document_web/src/app/features/dokumenty/`) jest podłączony do
rzeczywistego `document_api` dla listy/szczegółów dokumentu, wersji oraz listy/historii
szablonów Jasper. Poniżej tylko to, co faktycznie zostaje nieobsłużone.

## Potrzebne endpointy (nieistniejące jeszcze)

- `GET /api/dokumenty/:id/wersje/:wersja/podpis` — szczegóły podpisu: podpisujący (CN z
  certyfikatu), czas podpisu, odcisk certyfikatu, status OCSP, status CRL, znacznik czasu
  TSA. Backend `SzczegolyPodpisu` DTO już istnieje (`robert_neat.document_api.dto.dokumenty`)
  ale NIE ma kontrolera wystawiającego ten endpoint. Front obecnie renderuje te pola mockowo
  w dialogu "Szczegóły podpisu" (`dokument-szczegoly.ts`) — potrzebuje struktury 1:1 z tymi
  polami, gdy endpoint powstanie.
- `POST /api/szablony` — multipart upload nowego pliku `.jrxml` (+ `typDokumentuId`),
  orkiestruje zapis w `jasper_service` i wersjonowanie. Walidacja rozszerzenia `.jrxml` jest
  już robiona po stronie frontu (`szablony-strona.ts`), ale backend musi to też weryfikować.
