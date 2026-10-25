# Kontrakt: document_api ↔ frontend (moduł Wymiana EDI / XML)

Moduł Wymiana EDI/XML (`apps/document_web/src/app/features/wymiana/`) korzysta
obecnie wyłącznie z danych mockowych (`wymiana-mock.ts` dla historii importów/
eksportów, `core/mock/mock-data.ts` dla `MOCK_SPRAWY`/`MOCK_SZABLONY` użytych do
symulacji mapowania i akcji zbiorowych). Walidacja XSD/UBL, parsowanie plików
i uruchamianie akcji zbiorowych są w całości zasymulowane w przeglądarce
(`setTimeout`/`setInterval`), bez żadnego wywołania sieciowego.

Backend (`document_api`) już wystawia poniższe endpointy — brakuje wyłącznie
integracji po stronie frontendu.

## Endpointy już wystawione przez document_api (do podłączenia we frontendzie)

- `POST /api/wymiana/xml` — multipart upload komunikatu sprawy w formacie XML
  (`WymianaController`). Frontend obecnie całość mockuje w `ImportXml.onUpload`
  na podstawie nazwy pliku (plik zawierający „niepoprawn” lub bez rozszerzenia
  `.xml` symuluje błąd walidacji).
- `GET /api/wymiana/xml/historia` — historia importów/eksportów komunikatów XML
  do zakładki „Wymiana XML (EZD)”. Obecnie `MOCK_HISTORIA_XML` w `wymiana-mock.ts`.
- `POST /api/wymiana/ubl` — multipart upload faktury kosztowej w formacie
  UBL 2.1 (profil PEPPOL BIS Billing 3.0). Frontend obecnie mockuje w
  `ImportUbl.onUpload`.
- `GET /api/wymiana/ubl/historia` — analogicznie do historii XML, dla zakładki
  „Wymiana UBL/PEPPOL”. Obecnie `MOCK_HISTORIA_UBL` w `wymiana-mock.ts`.
- `POST /api/akcje-zbiorowe` — jeden generyczny endpoint, zwraca `202 Accepted`
  (żądanie jest tylko publikowane do RabbitMQ i przetwarzane asynchronicznie).
  Body (`UruchomAkcjeZbiorowaRequest`):
  `{ typAkcji, typCelu, idCelow: string[], kryteria?: object, parametry }`, gdzie
  `typAkcji` to `ZMIANA_STANU | WYSYLKA_PISM | GENEROWANIE_DOKUMENTOW |
AKTUALIZACJA_DANYCH | AKTUALIZACJA_ROL`, `typCelu` (`TypCeluAkcjiZbiorowej`)
  określa typ obiektów w `idCelow`, a `parametry` zależy od `typAkcji`:
  `ParametryZmianaStanu { nowyStan }`, `ParametryWysylkaPism { kanal }`
  (`KanalWysylki`), `ParametryGenerowanieDokumentow { szablonId }`. Odpowiedź
  (`UruchomAkcjeZbiorowaResponse`): `{ akcjaId }` do odpytywania statusu.
  Frontend (`akcje-zbiorowe.ts`) wciąż symuluje postęp lokalnie przez
  `setInterval`, bez wywołania API.
- `GET /api/akcje-zbiorowe/:akcjaId/status` — zwraca `StatusAkcjiZbiorowej`:
  `{ akcjaId, typAkcji, typCelu, status: "trwajaca"|"zakonczona", postep,
lacznie, przetworzone, bledne, wynikiElementow: [{ idCelu, status:
"sukces"|"blad", komunikat }] }`. Frontend powinien odpytywać ten endpoint
  (polling) po podłączeniu `POST /api/akcje-zbiorowe`.

## Uwagi

- `AkcjaZbiorowaProcessingService.wykonajGenerowanieDokumentu()` tworzy obecnie
  tylko metadane dokumentu + niepodpisaną wersję, bez realnego renderowania PDF
  przez `jasper_service` (świadomy zakres etapu, do zamknięcia razem z
  podłączeniem jasper_service — zob. `document_api_platnosci.md` / plan.md).
- Trzy karty akcji zbiorowych na stronie `akcje-zbiorowe` współdzielą listę
  spraw z `MOCK_SPRAWY`; backend `GET /api/sprawy` (już zaimplementowany i
  podłączony w module Sprawy) powinien zasilać tę samą listę zamiast mocka.
