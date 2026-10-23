# Kontrakt: document_api ↔ frontend (moduł Korespondencja)

Moduł Korespondencja (`apps/document_web/src/app/features/korespondencja/`) korzysta
obecnie wyłącznie z danych mockowych (`core/mock/mock-data.ts` — `MOCK_PRZESYLKI`,
stan trzymany w lokalnym `KorespondencjaStore` —
`features/korespondencja/korespondencja.store.ts`). Status przesyłki i historia
statusów są symulowane lokalnym automatem stanów (przycisk "Symuluj aktualizację
statusu"). Poniżej docelowe zapotrzebowanie na endpointy document_api, które mają
zastąpić mock i podłączyć się pod serwis `delivery_mock`.

## Potrzebne endpointy

- `GET /api/przesylki?status=<status>&kanal=<kanal>&kierunek=<kierunek>` — rejestr
  przesyłek z opcjonalnymi filtrami po `StatusPrzesylki`, `KanalWysylki` i
  `Przesylka.kierunek`. Używane w rejestrze (`''`, zakładki Wychodzące/Wpływające).
- `GET /api/przesylki/:id` — szczegóły przesyłki wraz z historią statusów
  (strona `:id`, oś czasu). Historia statusów nie istnieje jeszcze w modelu
  `Przesylka` — patrz uwagi.
- `POST /api/przesylki` — nadanie nowego pisma. Body: `{ sprawaId, dokumentId,
obywatelId, kanal }`. Backend powinien zweryfikować, że dokument jest podpisaną,
  aktualną wersją (`WersjaDokumentu.podpisana && wersja === aktualnaWersja`),
  wygenerować `numerReferencyjny` (prefiks zależny od kanału, np. `ADE/`, `PL`,
  `MAIL/`) i zwrócić nowo utworzoną `Przesylka` ze statusem `nadane`. Frontend
  mockuje to w `KorespondencjaStore.nadaj`.
- Aktualizacja statusu przesyłki — po stronie backendu już zaimplementowana jako
  odbiór zdarzenia RabbitMQ z `delivery_mock` (nie REST), zob.
  `currentwork/contracts/delivery_mock.md`. Frontend nie ma dziś żadnego
  REST/webhook endpointu do zasubskrybowania zmiany statusu — obecnie symuluje
  to przyciskiem w UI (`KorespondencjaStore.symulujAktualizacjeStatusu`);
  docelowe podłączenie pod rzeczywisty stan powinno odpytywać
  `GET /api/przesylki/:id` pollingowo na stronie szczegółów (historiaStatusow
  już tam jest zwracana).

## Uwagi

- Model `Przesylka` (pakiet `document-shared-dto`) ma już pola `kierunek` i
  `historiaStatusow`. Frontend (zakładki Wychodzące/Wpływające w rejestrze, oś czasu
  w szczegółach) filtruje/renderuje po tych polach na danych mockowych — do
  podłączenia pod `GET /api/przesylki` i `GET /api/przesylki/:id` zwracające te same
  pola z backendu.
- Automat statusów (`mozliweKolejneStatusy`/"Symuluj aktualizację statusu") modeluje
  dwa osobne cykle życia w zależności od `Przesylka.kierunek`: dla wychodzących
  nadane → doreczone/awizo → odebrane/zwrot (dostawa do adresata), dla wpływających
  nadane (zarejestrowano wpływ) → odebrane/zwrot (urząd przyjął pismo albo je
  odesłał nadawcy). Backend/`delivery_mock` powinien wystawiać odpowiadający,
  zależny od kierunku cykl życia statusów przed podłączeniem realnego API.
- Żaden mock obywatela nie ma wypełnionego `adresDoreczenEDE` — formularz nadania
  pisma nie blokuje wyboru kanału e-Doręczenia z tego powodu (tylko komunikat
  ostrzegawczy), ale backend powinien zweryfikować przed faktycznym wysłaniem przez
  ADE, czy adresat ma skrzynkę e-Doręczeń.
- Linki do `/sprawy/:id` i `/obywatele/:id` z rejestru i szczegółów przesyłki
  zakładają, że te trasy szczegółowe istnieją w modułach Sprawy/Obywatele.
