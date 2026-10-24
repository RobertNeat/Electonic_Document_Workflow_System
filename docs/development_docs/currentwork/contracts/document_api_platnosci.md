# Kontrakt: document_api ↔ frontend (moduł Płatności)

Moduł Płatności (`apps/document_web/src/app/features/platnosci/`) jest podłączony pod
rzeczywiste `document_api` przez `core/api/platnosci.api.ts` (`PlatnosciApi`) i
`features/platnosci/data/platnosci-store.ts` (`PlatnosciStore`). Jedyny pozostały
niezaadresowany endpoint:

- `POST /api/naleznosci/:id/wezwanie` — wygenerowanie wezwania do zapłaty dla
  przeterminowanej należności. Endpoint NIE istnieje po stronie `document_api` - wymaga
  `jasper_service`, który jeszcze nie istnieje (zob. Javadoc klasy `PlatnosciController`).
  Frontend (`platnosci-strona.ts`'s `otworzWezwanie`/`potwierdzWezwanie`) pozostaje
  świadomym mockiem z dialogiem potwierdzającym - realne generowanie dokumentu z szablonu
  ma wykonać moduł Dokumenty.
