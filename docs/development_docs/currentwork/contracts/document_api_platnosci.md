# Kontrakt: document_api ↔ frontend (moduł Płatności)

Moduł Płatności (`apps/document_web/src/app/features/platnosci/`) jest podłączony pod
rzeczywiste `document_api` przez `core/api/platnosci.api.ts` (`PlatnosciApi`) i
`features/platnosci/data/platnosci-store.ts` (`PlatnosciStore`). Jedyny pozostały
niezaadresowany endpoint:

- `POST /api/naleznosci/:id/wezwanie` — wygenerowanie wezwania do zapłaty dla
  przeterminowanej należności. Endpoint NIE istnieje po stronie `document_api`.
  `JasperServiceClient` już istnieje i jest podłączony (używany w akcji
  zbiorowej "generowanie dokumentów") - brakuje samego endpointu/kontrolera
  dla tej konkretnej operacji (zob. plan.md). Frontend (`platnosci-strona.ts`'s
  `otworzWezwanie`/`potwierdzWezwanie`) pozostaje świadomym mockiem z dialogiem
  potwierdzającym - realne generowanie dokumentu z szablonu ma wykonać moduł
  Dokumenty.
