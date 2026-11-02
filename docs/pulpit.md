---
title: Pulpit użytkownika
nav_order: 4
---

# Pulpit użytkownika

## Opis

Pulpit jest domyślną stroną startową aplikacji `document_web` — po zalogowaniu użytkownik trafia
na niego automatycznie, bez konieczności nawigowania przez menu. Każda rola (referent, kierownik
i inne role dostępne w systemie) widzi własny pulpit, zbudowany z jej własnych danych: sprawy,
terminy, należności i przesyłki są zawsze ograniczone do zalogowanego użytkownika (a nie do
wszystkich spraw w systemie). Pulpit nie jest konfigurowalny — każda rola widzi ten sam zestaw
kafelków i list, różni je jedynie zawartość wynikająca z przypisanych spraw.

## Co pokazuje

Górną część pulpitu zajmują cztery kafelki liczbowe:

- **Sprawy w toku** — liczba spraw przypisanych do zalogowanego referenta, które nie są zamknięte.
- **Terminy zbliżające się / przekroczone** — liczba spraw referenta z terminem ustawowym, do
  którego zostało 7 dni lub mniej (łącznie z terminami już przekroczonymi). Gdy liczba jest większa
  od zera, kafelek dodatkowo wyświetla etykietę „Wymaga uwagi”.
- **Nierozliczone należności (wydział)** — liczba należności w statusie nierozliczona, częściowo
  rozliczona lub przeterminowana, powiązanych ze sprawami wydziału referenta.
- **Przesyłki oczekujące na status** — liczba przesyłek w statusie nadane lub awizo, powiązanych ze
  sprawami wydziału referenta.

Pod kafelkami znajduje się tabela **Moje sprawy** — lista spraw referenta (znak sprawy, tytuł,
strona/obywatel, stan, data wszczęcia), z której kliknięcie wiersza przenosi do pełnych szczegółów
danej sprawy. Niżej umieszczona jest lista **Terminy ustawowe** — sprawy referenta posiadające
termin ustawowy, posortowane tak, że najpierw pojawiają się terminy przeterminowane, a następnie
najbliższe w czasie. Każdy wpis pokazuje datę terminu i etykietę w formie tagu: liczbę dni do
terminu, „Dziś” lub „Przeterminowane (N dni)” dla terminów już minionych — kolorystyka tagu
(czerwony dla przeterminowanych, żółty dla terminu w ciągu 7 dni, zielony dla pozostałych) odpowiada
pilności. Kliknięcie wpisu na liście terminów również prowadzi do szczegółów sprawy.

Nagłówek pulpitu wyświetla imię i nazwisko zalogowanego użytkownika.

## Szczegóły implementacji

Pulpit korzysta z rzeczywistych endpointów `document_api`: `GET /api/sprawy`, `GET /api/naleznosci`,
`GET /api/przesylki` oraz `GET /api/obywatele/:id` (do dociągnięcia nazwy strony dla każdej sprawy
widocznej w tabeli „Moje sprawy”). Żaden z tych endpointów nie wspiera natywnie filtrowania
potrzebnego pulpitowi, więc całe zawężanie wyników odbywa się po stronie klienta (w komponencie
`pulpit-strona`), nie na backendzie:

- `GET /api/sprawy` nie przyjmuje filtra po referencie. Frontend pociąga jedną stronę wyników
  (rozmiar strony 200, bez górnego limitu wymuszonego przez backend) i filtruje lokalnie po polu
  `referent`, porównując je dokładnie (exact-match stringa) z imieniem i nazwiskiem zalogowanego
  użytkownika. Dopasowanie nie jest rozmyte — jeśli referent sprawy zostanie zapisany w innej formie
  niż imię i nazwisko z konta Keycloak, sprawa nie pojawi się na pulpicie danego użytkownika. Przy
  skali danych demo (poniżej 200 spraw) filtrowanie działa w pełni poprawnie; gdyby liczba spraw w
  systemie przekroczyła rozmiar pobieranej strony, część spraw referenta mogłaby nie zostać
  uwzględniona.
- `GET /api/naleznosci` i `GET /api/przesylki` nie wspierają filtra po wielu statusach naraz (OR)
  ani filtra po wydziale. Frontend pociąga pełne listy bez filtra statusu, a następnie filtruje
  lokalnie: po zbiorze statusów uznawanych za „nierozliczone” / „oczekujące” oraz po wydziale,
  dopasowując należności i przesyłki do spraw przez pole `sprawaId` (join z listą spraw już
  pobraną przez pulpit).
- Token Keycloak nie zawiera atrybutu wydziału zalogowanego użytkownika. Pulpit przybliża wydział
  referenta wydziałem jego najnowszej sprawy (pierwsza sprawa na liście „Moje sprawy” po
  sortowaniu po dacie wszczęcia). Jeśli referent nie ma jeszcze żadnej sprawy, wydział nie jest
  znany i kafelki „Nierozliczone należności” oraz „Przesyłki oczekujące” pokazują zero.

## Ograniczenia

System nie generuje żadnych powiadomień o zbliżającym się lub przekroczonym terminie ustawowym —
ani e-mailem, ani push, ani w postaci komunikatu wewnątrz aplikacji. Pulpit pokazuje jedynie
aktualny stan terminów w momencie, w którym zostanie otwarty; jeśli nikt nie zaloguje się do
systemu, przekroczenie terminu przejdzie niezauważone.

Nagłówek pulpitu nie wyświetla stanowiska ani wydziału zalogowanego użytkownika — te informacje nie
są dostępne w tokenie logowania Keycloak, niezależnie od zalogowanego konta.

Kafelki „Nierozliczone należności” i „Przesyłki oczekujące na status” są wyłącznie liczbami — nie
mają przejścia dalej. Aby zobaczyć listy stojące za tymi liczbami, trzeba przejść przez osobne
menu Płatności lub Korespondencja.

Pulpit nie posiada „skrzynki zadań” w sensie dekretacji — pokazuje wyłącznie sprawy już przypisane
do referenta, a nie sprawy lub dokumenty czekające na przydzielenie lub decyzję.
