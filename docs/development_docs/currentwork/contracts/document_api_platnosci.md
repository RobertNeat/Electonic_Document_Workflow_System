# Kontrakt: document_api ↔ frontend (moduł Płatności)

Moduł Płatności (`apps/document_web/src/app/features/platnosci/`) jest podłączony pod
rzeczywiste `document_api` przez `core/api/platnosci.api.ts` (`PlatnosciApi`) i
`features/platnosci/data/platnosci-store.ts` (`PlatnosciStore`). Jedyny pozostały
niezaadresowany endpoint:

- `POST /api/naleznosci/:id/wezwanie` — wygenerowanie wezwania do zapłaty dla
  przeterminowanej należności. Endpoint NIE istnieje po stronie `document_api`.
  `jasper_service` już istnieje i działa (kompletny pipeline renderowania PDF),
  ale `document_api` go jeszcze nie konsumuje (brak `JasperServiceClient` i
  wpisu `jasper.service.base-url` w konfiguracji) — to jest właściwy bloker,
  nie brak samej usługi (zob. plan.md). Frontend (`platnosci-strona.ts`'s
  `otworzWezwanie`/`potwierdzWezwanie`) pozostaje świadomym mockiem z dialogiem
  potwierdzającym - realne generowanie dokumentu z szablonu ma wykonać moduł
  Dokumenty.
