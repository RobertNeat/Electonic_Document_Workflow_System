# Przepływ: Korespondencja i wysyłka pism

## Zakres

Wysyłka podpisanego pisma do adresata przez wybranego dostawcę (e-Doręczenia/ADE, operator
pocztowy, e-mail), śledzenie statusu doręczenia oraz rejestracja przesyłek wpływających i
wychodzących, z perspektywy `document_web` (moduł `features/korespondencja`), `document_api`
(`PrzesylkaController`/`KorespondencjaService`) i `delivery_mock` (REST + panel Thymeleaf).

## Kroki przepływu (UI)

1. Rejestr korespondencji, ekran `/korespondencja` (`korespondencja-strona`) — zakładki
   "wychodząca"/"wpływająca", filtr statusu i kanału — endpoint REST użyty:
   `GET /api/przesylki?kierunek=&status=&kanal=&obywatelId=`.
2. Przejście do formularza nadania pisma, ekran `/korespondencja/wyslij`
   (`korespondencja-wyslij`):
   - wybór sprawy — `GET /api/sprawy?rozmiarStrony=200`,
   - dociągnięcie dokumentów sprawy po wyborze sprawy (filtrowane po stronie klienta do wersji
     podpisanej z `statusPodpisu == 'poprawny'`) — `GET /api/dokumenty?sprawaId=...`,
   - wybór adresata (domyślnie obywatel przypisany do sprawy, edytowalny) — lista z
     `GET /api/obywatele`,
   - wybór kanału wysyłki (e-Doręczenia / operator pocztowy / e-mail) — pole formularza, bez
     dodatkowego wywołania; UI pokazuje ostrzeżenie, jeśli obywatel nie ma adresu ADE, zanim
     backend odrzuci żądanie.
   - wysłanie — `POST /api/przesylki` (`UtworzPrzesylkeRequest{sprawaId, dokumentId, obywatelId,
     kanal}`), backend nadaje `numerReferencyjny` i zapisuje `status=nadane`,
     `kierunek=wychodzaca` (na trwałe, pole nie jest przyjmowane od klienta).
3. Szczegóły przesyłki, ekran `/korespondencja/:id` (`korespondencja-szczegoly`) —
   `GET /api/przesylki/:id`, odpytywane pollingiem co 8 s (brak REST endpointu do
   zasubskrybowania zmiany); karta pokazuje historię statusów (`historiaStatusow`) na
   `p-timeline`, wraz z danymi sprawy (`GET /api/sprawy/:id`), dokumentu
   (`GET /api/dokumenty/:id`) i obywatela (`GET /api/obywatele/:id`).
4. Masowa wysyłka pism (akcja zbiorowa), ekran modułu "Akcje zbiorowe" — `POST /api/akcje-zbiorowe`
   (typ `WYSYLKA_PISM`), odpytywane pollingiem `GET /api/akcje-zbiorowe/:akcjaId/status` co 1,2 s;
   wewnętrznie wywołuje ten sam `KorespondencjaService.nadajPismo` per sprawa — ten sam skutek i
   te samo ograniczenie opisane w sekcji "Braki" poniżej.
5. Symulacja zmiany statusu przesyłki (nadane → doręczone/odebrane/zwrot/awizo) — **wyłącznie w
   panelu Thymeleaf `delivery_mock`** (`GET /przesylki`, `GET /przesylki/{id}`, formularz
   "Symuluj zmianę statusu" → `POST /przesylki/{id}/symuluj-status`), port 10624. Jest to osobna
   aplikacja z katalogu `./apps`, więc operacja odbywa się "w aplikacji", choć nie w
   `document_web`. Po zapisaniu zmiany `delivery_mock` publikuje zdarzenie RabbitMQ
   (`przesylki-status.exchange` / `przesylki-status.queue`), które `document_api`
   (`PrzesylkaStatusuConsumer`) konsumuje i aktualizuje `Przesylka` po `numerReferencyjny` —
   dalej widoczne w `document_web` przy następnym tiku pollingu (krok 3).

## Dane testowe do manualnego przejścia przepływu

Seed demo (`apps/document_api/.../demo/100-seed.xml`, `apps/delivery_mock/.../demo/100-seed.xml`):

- Sprawa `spr-1`, znak sprawy **WO.6220.14.2026** — "Wniosek o wydanie decyzji środowiskowej",
  wydział Ochrony Środowiska, referent Katarzyna Zielińska.
- Obywatel `ob-1` — **Jan Kowalski**, adres do e-Doręczeń (ADE): `AE:PL-12345-67890-ABCDE-12`.
- Dokument `d-2` — "Decyzja środowiskowa" (aktualna wersja 2) przypisany do `spr-1` — do wysyłki
  formularz wymaga wersji podpisanej (`statusPodpisuAktualnej='poprawny'`); jeśli żadna wersja
  jeszcze nie jest podpisana w danej instancji, trzeba ją najpierw podpisać w module Dokumenty
  (krok 0 przed testem wysyłki).
- Przesyłka już nadana demo: `p-1`, numer referencyjny **ADE/2026/00123**, kanał e-doreczenia,
  status `doreczone` — ma odpowiadający wiersz `pd-1` w `delivery_mock` z tym samym numerem, więc
  historia statusów jest kompletna i widoczna od razu w szczegółach `/korespondencja/p-1`.
- Druga przesyłka demo: `p-2`, numer **PL2026000998**, kanał `operator_pocztowy`, status
  `nadane`, obywatel `ob-2` (Anna Nowak, sprawa `spr-2` OR.1510.3.2026) — odpowiadający wiersz
  `pd-2` w `delivery_mock`; można na niej przetestować symulację "doręczono"/"zwrot"/"awizo" przez
  panel Thymeleaf i obserwować propagację do rejestru `document_web`.
- Format nowo generowanych numerów referencyjnych (`KorespondencjaService.generujNumerReferencyjny`):
  `ADE/2026/00124`, `PL2026000999`, `MAIL/2026/00001` (kolejny wolny numer w danym roku/kanale).

## Braki / działania niemożliwe z UI

1. **Nowo nadane pismo nie da się śledzić/symulować bez surowego żądania REST do
   `delivery_mock`.** `KorespondencjaService.nadajPismo`
   (`apps/document_api/src/main/java/robert_neat/document_api/service/KorespondencjaService.java:118-168`)
   zapisuje przesyłkę wyłącznie lokalnie w schemie `core` i **nigdy nie woła**
   `POST /api/przesylki-dostawy` w `delivery_mock` — potwierdzone też w
   `docs/development_docs/currentwork/contracts/delivery_mock.md` ("Żaden inny moduł... jeszcze
   NIE woła do tego API"). `PrzesylkiWidokController` (panel Thymeleaf) ma tylko `GET /przesylki`,
   `GET /przesylki/{id}` i `POST /przesylki/{id}/symuluj-status` — **brak formularza tworzenia
   nowej przesyłki dostawy w UI**. Dla przesyłki utworzonej w `document_web` (poza seedem demo)
   `delivery_mock` nie ma więc żadnego odpowiadającego jej rekordu o tym samym
   `numerReferencyjny`, dopóki ktoś nie wywoła ręcznie
   `POST /api/przesylki-dostawy {"numerReferencyjny": "<ten z document_api>", "kanal": "..."}`
   wprost na `delivery_mock` (surowe REST, poza każdą aplikacją `./apps`). Tylko po tym kroku
   panel Thymeleaf może cokolwiek symulować dla tej przesyłki. Dwie seedowane demo-przesyłki
   (`p-1`/`pd-1`, `p-2`/`pd-2`) działają end-to-end wyłącznie dlatego, że ich `numerReferencyjny`
   zostały ręcznie zgrane 1:1 w obu changelogach Liquibase — to nie jest mechanizm runtime.
2. **Rejestracja przesyłki PRZYCHODZĄCEJ (wpływającej) nie ma żadnego odpowiednika — ani w UI, ani
   w API.** `UtworzPrzesylkeRequest`
   (`apps/document_api/src/main/java/robert_neat/document_api/dto/korespondencja/UtworzPrzesylkeRequest.java`)
   nie ma pola `kierunek`; `KorespondencjaService.nadajPismo` ustawia
   `przesylka.setKierunek("wychodzaca")` na trwałe (linia 154). Nie istnieje żaden inny endpoint w
   `document_api` do zapisania przesyłki wpływającej. Zakładka "wpływająca" w rejestrze
   `document_web` (`korespondencja-strona.ts`, `Kierunek`) jest więc czystym filtrem odczytu na
   zbiorze, który nigdy nie może się zapełnić przez UI/API — dotyczy to zarówno korespondencji
   papierowej, jak i elektronicznej wpływającej.
3. **Status przesyłki nie odświeża się przez żaden "push"/webhook do UI** — `document_web`
   odpytuje `GET /api/przesylki/:id` co 8 s (polling), bo `document_api` nie wystawia endpointu do
   subskrybowania zdarzenia; to jest świadomy, działający kompromis (nie blokada), ale oznacza, że
   zmiana widoczna jest z opóźnieniem do 8 s, nigdy natychmiast.

**Najważniejsza luka:** punkt 1 — dla każdej przesyłki nadanej realnie przez `document_web` (poza
dwiema wpisami demo), manualne przejście pełnego przepływu "wysłano → zmiana statusu →
widoczne w UI" wymaga surowego wywołania REST do `delivery_mock`
(`POST /api/przesylki-dostawy`) z numerem referencyjnym skopiowanym ręcznie z odpowiedzi
`document_api`, bo żadna aplikacja z `./apps` nie tworzy tego powiązania automatycznie.
