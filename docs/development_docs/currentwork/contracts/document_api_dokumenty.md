# Kontrakt: document_api ↔ frontend (moduł Dokumenty i szablony)

Moduł Dokumenty (`apps/document_web/src/app/features/dokumenty/`) jest podłączony do
rzeczywistego `document_api` dla listy/szczegółów dokumentu, wersji oraz listy/historii
szablonów Jasper. Poniżej tylko to, co faktycznie zostaje nieobsłużone.

## Potrzebne endpointy (nieistniejące jeszcze)

- `POST /api/szablony` — multipart upload nowego pliku `.jrxml` (+ `typDokumentuId`),
  orkiestruje zapis w `jasper_service` i wersjonowanie. Walidacja rozszerzenia `.jrxml` jest
  już robiona po stronie frontu (`szablony-strona.ts`), ale backend musi to też weryfikować.
