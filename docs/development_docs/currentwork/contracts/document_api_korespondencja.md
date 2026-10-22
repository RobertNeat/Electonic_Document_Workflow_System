# Kontrakt: document_api ↔ frontend (moduł Korespondencja)

Moduł Korespondencja (`apps/document_web/src/app/features/korespondencja/`) korzysta
obecnie wyłącznie z danych mockowych (`core/mock/mock-data.ts` — `MOCK_PRZESYLKI`,
stan trzymany w lokalnym `KorespondencjaStore` —
`features/korespondencja/korespondencja.store.ts`). Status przesyłki i historia
statusów są symulowane lokalnym automatem stanów (przycisk "Symuluj aktualizację
statusu"). Poniżej docelowe zapotrzebowanie na endpointy document_api, które mają
zastąpić mock i podłączyć się pod serwis `delivery_mock`.

## Potrzebne endpointy

- `GET /api/przesylki?status=<status>&kanal=<kanal>` — rejestr przesyłek z
  opcjonalnymi filtrami po `StatusPrzesylki` i `KanalWysylki`. Używane w rejestrze
  (`''`). Docelowo powinien też przyjmować filtr kierunku (patrz uwagi).
- `GET /api/przesylki/:id` — szczegóły przesyłki wraz z historią statusów
  (strona `:id`, oś czasu). Historia statusów nie istnieje jeszcze w modelu
  `Przesylka` — patrz uwagi.
- `POST /api/przesylki` — nadanie nowego pisma. Body: `{ sprawaId, dokumentId,
obywatelId, kanal }`. Backend powinien zweryfikować, że dokument jest podpisaną,
  aktualną wersją (`WersjaDokumentu.podpisana && wersja === aktualnaWersja`),
  wygenerować `numerReferencyjny` (prefiks zależny od kanału, np. `ADE/`, `PL`,
  `MAIL/`) i zwrócić nowo utworzoną `Przesylka` ze statusem `nadane`. Frontend
  mockuje to w `KorespondencjaStore.nadaj`.
- Aktualizacja statusu przesyłki — webhook/callback z `delivery_mock` (np.
  `POST /api/przesylki/:id/status` lub polling `GET /api/przesylki/:id/status`)
  informujący o zmianie statusu (`doreczone`/`odebrane`/`zwrot`/`awizo`) wraz z
  datą zdarzenia. Frontend obecnie symuluje to przyciskiem w UI
  (`KorespondencjaStore.symulujAktualizacjeStatusu`), docelowo powinien albo
  nasłuchiwać webhooka (np. przez WebSocket/SSE), albo odpytywać endpoint
  pollingowo na stronie szczegółów.

## Uwagi

- Model `Przesylka` (core/models/przesylka.model.ts) nie przechowuje **kierunku**
  przesyłki (wpływająca/wychodząca). Frontend traktuje cały `MOCK_PRZESYLKI` jako
  wychodzące i pokazuje osobną zakładkę "Wpływające" z informacją, że dane nie są
  jeszcze dostępne. Backend powinien dodać pole `kierunek` (lub osobny endpoint dla
  przesyłek wpływających) przed podłączeniem realnego API.
- Model `Przesylka` nie przechowuje **historii statusów** — tylko ostatni status i
  `dataStatusu`. Oś czasu w szczegółach przesyłki jest budowana przez frontend
  lokalnie (`dataNadania` + bieżący `dataStatusu`, jeśli różny). Backend/`GET
/api/przesylki/:id` powinien zwracać pełną listę wpisów `{ status, data }`.
- Żaden mock obywatela nie ma wypełnionego `adresDoreczenEDE` — formularz nadania
  pisma nie blokuje wyboru kanału e-Doręczenia z tego powodu (tylko komunikat
  ostrzegawczy), ale backend powinien zweryfikować przed faktycznym wysłaniem przez
  ADE, czy adresat ma skrzynkę e-Doręczeń.
- Linki do `/sprawy/:id` i `/obywatele/:id` z rejestru i szczegółów przesyłki
  zakładają, że te trasy szczegółowe istnieją w modułach Sprawy/Obywatele.
