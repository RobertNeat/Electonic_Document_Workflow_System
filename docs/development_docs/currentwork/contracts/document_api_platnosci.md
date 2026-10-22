# Kontrakt: document_api ↔ frontend (moduł Płatności)

Moduł Płatności (`apps/document_web/src/app/features/platnosci/`) korzysta obecnie
wyłącznie z danych mockowych (`core/mock/mock-data.ts`, stan trzymany w lokalnym
`PlatnosciStore` — `features/platnosci/data/platnosci-store.ts`). Poniżej docelowe
zapotrzebowanie na endpointy document_api.

## Potrzebne endpointy

- `GET /api/naleznosci?status=<status>` — rejestr należności z opcjonalnym filtrem
  po `StatusRozliczenia` (nierozliczona/czesciowo_rozliczona/rozliczona/przeterminowana).
  Używane w rejestrze należności (`''`).
- `GET /api/naleznosci/:id` — szczegóły pojedynczej należności (strona `:id`).
- `GET /api/wplaty?naleznoscId=<id>` — wpłaty powiązane z konkretną należnością
  (historia wpłat na stronie szczegółów) oraz bez filtru — pełny rejestr wpłat
  (strona `wplaty`).
- `POST /api/wplaty/:id/dopasuj` — body `{ naleznoscId: string }`. Ręczne dopasowanie
  wpłaty do należności; backend powinien atomowo zaktualizować `Wplata.naleznoscId`
  oraz `Naleznosc.kwotaRozliczona`/`status` (frontend obecnie liczy to lokalnie
  w `PlatnosciStore.dopasujRecznie`, co należy zastąpić wywołaniem API).
- `POST /api/import-camt053` — multipart upload pliku XML (CAMT.053, ISO 20022).
  Odpowiedź: liczba rozpoznanych transakcji, liczba dopasowanych automatycznie,
  liczba wymagających rekoncyliacji ręcznej oraz lista utworzonych wpłat (do
  wyświetlenia w podsumowaniu importu na stronie `import-wyciagu`). Obecnie
  frontend to w pełni mockuje w `PlatnosciStore.importujWyciag` (nie parsuje XML).
- `POST /api/naleznosci/:id/wezwanie` — wygenerowanie wezwania do zapłaty dla
  przeterminowanej należności. W tym module jest to tylko przycisk z dialogiem
  potwierdzającym (mock) — realne generowanie dokumentu z szablonu ma wykonać
  moduł Dokumenty; endpoint powinien prawdopodobnie delegować do istniejącego
  mechanizmu generowania pism z `SzablonJasper` (`td-4` / `wezwanie_do_zaplaty.jrxml`
  w mockach).

## Uwagi

- Linki do `/sprawy/:id` i `/obywatele/:id` z rejestru i szczegółów należności
  zakładają, że te trasy szczegółowe istnieją w modułach Sprawy/Obywatele — w chwili
  pisania tego kontraktu te moduły mają jeszcze tylko trasę `''` (placeholder).
- `Naleznosc.sprawaId` i `Naleznosc.obywatelId` są w mockach wyświetlane jako surowe
  id (brak wzbogacenia o `znakSprawy`/nazwę obywatela) — do rozważenia, czy
  `GET /api/naleznosci` powinien zwracać już zdenormalizowane pola (`znakSprawy`,
  `obywatelNazwa`) czy frontend ma je dociągać osobnym zapytaniem (batch
  `GET /api/obywatele?ids=...`, analogicznie do kontraktu modułu Pulpit).
