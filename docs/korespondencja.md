---
title: Korespondencja i wysyłka pism
nav_order: 7
---

# Korespondencja i wysyłka pism

## Opis

Moduł Korespondencja prowadzi rejestr przesyłek wychodzących i wpływających oraz pozwala nadać
podpisane pismo do adresata (mieszkańca lub podmiotu) wybranym kanałem wysyłki: e-Doręczenia (ADE),
operator pocztowy (przesyłka hybrydowa — druk i nadanie listu poleconego) albo e-mail jako kanał
pomocniczy, który nie ma skutków prawnych doręczenia. Po nadaniu pisma moduł pozwala śledzić status
jego doręczenia aż do stanu końcowego.

## Role

Z modułu korzysta **referent** — nadaje pisma w ramach prowadzonych spraw i śledzi status ich
doręczenia.

## Przebieg pracy

Rejestr korespondencji ma dwie zakładki — "Wychodzące" i "Wpływające" — oraz filtry statusu i
kanału wysyłki. Nadanie nowego pisma zaczyna się przyciskiem **"Nadaj pismo"**, który otwiera
formularz "Nadanie nowego pisma":

1. **Sprawa** — wybór sprawy, do której przypisane jest pismo.
2. **Podpisany dokument** — lista pokazuje tylko wersje dokumentu ze statusem podpisu "poprawny";
   wersje podpisane certyfikatem wygasłym lub unieważnionym nie pojawiają się na liście, mimo że
   formalnie są podpisane.
3. **Adresat** — domyślnie strona przypisana do sprawy, edytowalna.
4. **Kanał wysyłki** — e-Doręczenia, operator pocztowy albo e-mail.

![Podpisanie decyzji środowiskowej]({{ '/media/3. wysyłka korespondencji/1. podpisanie decyzji środowiskowej.png' | relative_url }})

Warunkiem wyboru dokumentu do wysyłki jest posiadanie przez niego podpisanej, poprawnej wersji —
zob. [Dokumenty, szablony i podpis elektroniczny](dokumenty-i-podpis.md).

![Wykonanie wysyłki korespondencji e-doręczenia]({{ '/media/3. wysyłka korespondencji/2. wykonanie wysyłki korespondencji e-doręczenia.png' | relative_url }})

Wysyłka kanałem e-Doręczenia wymaga, aby adresat miał zarejestrowany adres do e-Doręczeń (ADE).
Jeśli strona nie ma adresu ADE, aplikacja ostrzega o tym w formularzu, ale ostateczną kontrolę
wykonuje backend — próba nadania pisma kanałem e-Doręczenia bez adresu ADE u adresata jest
odrzucana. Adres ADE strony widoczny jest na jej [Karcie obywatela/podmiotu](karta-obywatela.md).

Po nadaniu pisma wyświetla się karta potwierdzenia "Pismo zostało nadane" z numerem referencyjnym
przesyłki i przyciskiem **"Przejdź do szczegółów przesyłki"**.

![Potwierdzenie wykonania wysyłki e-doręczenia]({{ '/media/3. wysyłka korespondencji/3. potwierdzenie wykonania wysyłki e-doręczenia.png' | relative_url }})

Ekran szczegółów przesyłki pokazuje numer referencyjny, dane przesyłki (kanał, skutki prawne
kanału, sprawę, dokument, adresata, datę nadania) oraz kartę "Śledzenie statusu" w formie osi
czasu. Status przesyłki odświeża się na tym ekranie automatycznie co 8 sekund, bez konieczności
ręcznego przeładowania strony.

![Szczegóły przesyłki]({{ '/media/3. wysyłka korespondencji/4. szczegóły przesyłki.png' | relative_url }})

Do zmiany statusu przesyłki (np. na "doręczone", "odebrane", "zwrot" albo "awizo") służy panel
symulacji doręczeń `delivery_mock` — osobny system demonstracyjny, dostępny pod własnym adresem,
niewymagający logowania. Pozwala on wybrać przesyłkę i ręcznie zasymulować zmianę jej statusu wraz
z opcjonalnym opisem zdarzenia; zmiana widoczna jest następnie na ekranie szczegółów przesyłki w
`document_web` po kolejnym cyklu odświeżania.

Pełny przebieg procesu — od podpisania dokumentu, przez nadanie pisma, po śledzenie statusu
przesyłki — pokazuje poniższa animacja.

![Przebieg wysyłki korespondencji]({{ '/media/3. wysyłka korespondencji/wysyłka_korespondencji.gif' | relative_url }})

## Szczegóły implementacji

Zmiana statusu przesyłki jest propagowana asynchronicznie: `delivery_mock` publikuje zdarzenie
zmiany statusu na RabbitMQ, a `document_api` konsumuje je i aktualizuje stan przesyłki w swojej
bazie. Nie jest to synchroniczny callback REST — `document_api` nie wystawia żadnego endpointu do
wyzwolenia lub zasubskrybowania tej zmiany. Po stronie frontendu ekran
szczegółów przesyłki odpytuje status przesyłki pollingiem (żądanie co 8 sekund), bo nie ma
mechanizmu powiadamiania w czasie rzeczywistym między `document_web` a `document_api`.

## Ograniczenia

Pismo nadane z poziomu `document_web` nie pojawia się automatycznie w panelu symulacji
`delivery_mock` — dotyczy to wszystkich przesyłek poza tymi, które zostały z góry przygotowane w
danych demonstracyjnych. `document_api` zapisuje nowo nadaną przesyłkę wyłącznie we własnej bazie i
nie zgłasza jej do `delivery_mock`, więc dla pisma nadanego samodzielnie nie da się zademonstrować
zmiany statusu przez panel symulacji — przesyłka pozostanie widoczna w `document_web` ze statusem
"nadane" na stałe.

Rejestracja przesyłki przychodzącej (wpływającej) nie jest możliwa z poziomu aplikacji w żadnej
formie — zakładka "Wpływające" w rejestrze korespondencji nie ma żadnego formularza ani akcji
pozwalającej zapisać taką przesyłkę i pozostaje pusta.

Status przesyłki nie odświeża się natychmiast — ekran szczegółów przesyłki odpytuje `document_api`
co 8 sekund, więc zmiana statusu zasymulowana w panelu `delivery_mock` staje się widoczna w
`document_web` z opóźnieniem do 8 sekund.
