# Przepływ: Logowanie i administracja

## Zakres

Scenariusz testowy do manualnego przejścia: logowanie przez Keycloak, wylogowanie, oraz — jako
administrator — zarządzanie użytkownikami/rolami, przegląd struktury organizacyjnej, edycja
słowników, edycja nazwy/opisu definicji obiegu i przegląd dziennika zdarzeń. Aplikacja: `document_web`
(adres `http://localhost:10600`), strona logowania Keycloaka (`http://localhost:10681`).

## Aktorzy i role

| Login w Keycloaku    | Rola          | Hasło                                                                                               |
| -------------------- | ------------- | --------------------------------------------------------------------------------------------------- |
| `demo.administrator` | administrator | wspólne dla wszystkich kont demo, ze zmiennej `KEYCLOAK_DEMO_USER_PASSWORD` (domyślnie `change-me`) |
| `demo.referent`      | referent      | jak wyżej                                                                                           |
| `demo.kierownik`     | kierownik     | jak wyżej                                                                                           |
| `demo.ksiegowosc`    | ksiegowosc    | jak wyżej                                                                                           |

Do tego przepływu potrzebny jest tylko `demo.administrator` — pozostałe trzy konta są przydatne
tylko do zweryfikowania, że ekran Administracja jest dla nich niewidoczny (krok 6).

> **Uwaga:** profile widoczne w ekranie "Użytkownicy i role" (np. `demo.kancelaria`, `jan.kaminski`)
> **nie mają** odpowiadającego konta w Keycloaku — nie da się na nich zalogować. Są to tylko dane w
> tabeli profili domenowych, niezależnej od kont logowania. Do testu logowania używaj wyłącznie
> czterech loginów z tabeli powyżej.

## Kroki przepływu

1. **Wejście na niezalogowaną stronę** — otwórz `http://localhost:10600`. Aplikacja przekieruje na
   ekran logowania z jednym przyciskiem **"Zaloguj się"**.
2. **Logowanie** — kliknij "Zaloguj się". Nastąpi przekierowanie na stronę logowania Keycloaka. Wpisz
   login `demo.administrator` i wspólne hasło demo. Po zalogowaniu wracasz do `document_web`, na
   ekran **Pulpit**.
3. **Sprawdzenie widoczności menu Administracja** — w lewym menu bocznym, zalogowany jako
   `demo.administrator`, powinna być widoczna pozycja **"Administracja"** jako ostatnia na liście (po
   Pulpit, Sprawy, Obywatele, Dokumenty i szablony, Korespondencja, Płatności, Wymiana EDI / XML).
   Jeśli chcesz zweryfikować, że inne role jej nie widzą: wyloguj się (patrz krok 9) i zaloguj ponownie
   jako `demo.referent`, `demo.kierownik` lub `demo.ksiegowosc` — pozycja "Administracja" nie pojawi
   się w menu dla żadnego z nich.
4. **Panel Administracja** — kliknij "Administracja". Zobaczysz 5 kart: "Użytkownicy i role",
   "Struktura organizacyjna", "Słowniki", "Definicje obiegów", "Dziennik zdarzeń".
5. **Użytkownicy i role** — kliknij kartę "Użytkownicy i role":
   - zobaczysz listę profili (login, imię i nazwisko, wydział, stanowisko, role),
   - kliknij przycisk **"Dodaj użytkownika"**, wypełnij dialog (login, imię, nazwisko, e-mail,
     wydział, stanowisko, role domenowe) i zatwierdź — nowy profil pojawi się na liście,
   - na wierszu istniejącego użytkownika zmień wartość w polu wyboru ról (multiselect) — zmiana
     zapisuje się natychmiast,
   - zaznacz checkboxem kilka wierszy i użyj przycisku zbiorczej zmiany ról (dialog "Zarządzaj
     rolami"), żeby nadać tę samą rolę kilku profilom naraz.
   - **Ważne:** zmiana roli w tym ekranie zmienia tylko profil domenowy w `document_web` — **nie**
     zmienia, co dany użytkownik faktycznie może zrobić w systemie. O realnych uprawnieniach decyduje
     wyłącznie rola przypisana kontu w Keycloaku (zob. "Znane ograniczenia", pkt 1).
6. **Struktura organizacyjna** — kliknij kartę "Struktura organizacyjna". Zobaczysz drzewo:
   wydział → stanowiska. To wyłącznie podgląd — nie ma tu żadnego przycisku edycji, dla żadnej roli.
7. **Słowniki** — kliknij kartę "Słowniki", przejdź między czterema zakładkami: "JRWA", "Typy
   dokumentów", "Dostawcy wysyłki", "Stawki opłat". Na każdej zakładce kliknij ikonę ołówka
   ("Edytuj") przy wybranym wierszu, zmień wartość w dialogu (np. "Edycja pozycji JRWA") i zapisz.
   Przycisk "Edytuj" jest widoczny tylko dla roli administrator — zalogowany jako `demo.referent`
   zobaczy te same listy, ale bez przycisków edycji.
8. **Definicje obiegów** — kliknij kartę "Definicje obiegów". Na karcie obiegu kliknij
   "Podgląd stanów", żeby zobaczyć sekwencję stanów i przejść w dialogu. Jako administrator kliknij
   też "Edytuj" — dialog pozwala zmienić tylko nazwę i opis obiegu, nie stany ani przejścia.
9. **Dziennik zdarzeń** — kliknij kartę "Dziennik zdarzeń". Ustaw filtr typu operacji i zakres dat,
   sprawdź listę zdarzeń. Przycisk "Wyczyść filtry" resetuje oba filtry. Ten ekran jest widoczny dla
   każdej zalogowanej roli, nie tylko administratora.
10. **Wylogowanie** — kliknij przycisk wylogowania w górnym pasku `document_web`. Zostaniesz
    przekierowany przez stronę wylogowania Keycloaka z powrotem na ekran logowania.
11. **Konsola administracyjna Keycloaka** (osobny system, nie `document_web`) — pod adresem
    `http://localhost:10681`, logowanie kontem administracyjnym Keycloaka (login
    `KEYCLOAK_ADMIN_USERNAME`, domyślnie `admin`; hasło `KEYCLOAK_ADMIN_PASSWORD`, domyślnie
    `change-me`). Tu zarządza się realnymi kontami logowania (hasła, włączenie/wyłączenie konta,
    przypisanie roli realmu) — to inny ekran niż "Użytkownicy i role" z kroku 5 i inny zestaw danych.

## Dane testowe do manualnego przejścia przepływu

Przykładowe pozycje do edycji w kroku 7 (zakładka Słowniki): symbol JRWA `1510` — "Skargi i
wnioski"; dostawca wysyłki "e-Doręczenia"; stawka opłaty "Opłata skarbowa — pełnomocnictwo" (17,00
zł).

Struktura organizacyjna widoczna w kroku 6: Wydział Ochrony Środowiska (kierownik Katarzyna
Zielińska), Wydział Organizacyjny (kierownik Piotr Lewandowski), Wydział Księgowości (kierownik
Marta Wiśniewska), Wydział Komunikacji.

## Znane ograniczenia (czego nie da się zrobić w aplikacji)

1. **Rola zmieniona w ekranie "Użytkownicy i role" nie wpływa na faktyczne uprawnienia.** O tym, co
   dany użytkownik może zrobić (np. podpisać dokument, zaimportować wyciąg bankowy), decyduje
   wyłącznie rola przypisana jego kontu w konsoli Keycloaka (krok 11) — a nie rola widoczna w tym
   ekranie. Oba zbiory danych (profile w `document_web` i konta w Keycloaku) nie są automatycznie
   synchronizowane: dodanie profilu w kroku 5 nie tworzy konta logowania w Keycloaku, i odwrotnie.
2. **Nie da się dodać nowego wydziału/stanowiska ani zmienić kierownika wydziału** — ekran "Struktura
   organizacyjna" jest tylko podglądem, bez żadnego przycisku edycji.
3. **Nie da się dodać nowej pozycji w żadnym z czterech słowników** — tylko edycja istniejących
   wierszy (ikona ołówka). Brak przycisku "Dodaj" w całym ekranie Słowniki.
4. **Nie da się utworzyć nowej definicji obiegu, ani zmienić jej stanów/przejść/wymaganych
   ról/dokumentów** — dialog "Edytuj" w ekranie "Definicje obiegów" pozwala zmienić tylko nazwę i
   opis. Nowy rodzaj sprawy wymaga działania poza aplikacją.
5. **Nie da się edytować profilu użytkownika po utworzeniu** (zmiana wydziału, stanowiska, imienia,
   nazwiska) — jedyna możliwa zmiana istniejącego profilu to lista ról. Nie da się też usunąć/
   dezaktywować profilu z tego ekranu.
