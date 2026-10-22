# Kontrakt: document_api ↔ frontend (moduł Pulpit)

Moduł Pulpit (`apps/document_web/src/app/features/pulpit/`) obecnie korzysta wyłącznie
z danych mockowych (`core/mock/mock-data.ts` + lokalny `pulpit-mock.ts`). Poniżej
docelowe zapotrzebowanie na endpointy document_api, gdy backend zyska zasoby domenowe.

## Potrzebne endpointy

- `GET /api/sprawy?referent=<login>&zamknieta=false` — sprawy w toku przypisane do
  zalogowanego referenta (do liczników "Sprawy w toku" i tabeli "Moje sprawy").
- `GET /api/sprawy?referent=<login>&terminUstawowySortuj=asc` — sprawy z ustawionym
  `terminUstawowy`, posortowane rosnąco, do sekcji "Terminy ustawowe". Alternatywnie
  może to być filtr po stronie klienta na wyniku powyższego zapytania.
- `GET /api/naleznosci?status=nierozliczona,czesciowo_rozliczona,przeterminowana&wydzial=<wydzial>`
  — liczba nierozliczonych należności wydziału do kafelka.
- `GET /api/przesylki?status=nadane,awizo` — przesyłki oczekujące na zmianę statusu.
- `GET /api/obywatele/:id` (lub batch `GET /api/obywatele?ids=...`) — do wyświetlenia
  nazwy strony w tabeli "Moje sprawy" bez ładowania całej listy obywateli na froncie.

## Uwagi

- Pulpit zakłada, że `Sprawa.referent` jest już przypisany na backendzie jako pełne
  imię i nazwisko referenta (tak jak w obecnym modelu) — jeśli backend wprowadzi
  `referentId`, frontend Pulpitu będzie wymagał dopasowania po id, nie po nazwie.
- Liczniki na Pulpicie są per-wydział dla należności/przesyłek, a per-referent dla
  spraw i terminów — warto to zachować w kontrakcie API (parametry `referent` vs
  `wydzial`), żeby nie trzeba było przeliczać agregatów po stronie klienta na dużych
  zbiorach danych.
