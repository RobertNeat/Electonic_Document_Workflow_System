---
title: Karta obywatela/podmiotu
nav_order: 9
---

# Karta obywatela/podmiotu

## Opis

Moduł **Obywatele** pozwala wyszukać osobę fizyczną lub podmiot (firmę, instytucję) zarejestrowany
w systemie i otworzyć jego ujednoliconą kartę. Wyszukiwanie jest częściowe i niewrażliwe na
wielkość liter — działa po imieniu, nazwisku, nazwie podmiotu, numerze PESEL oraz numerze NIP,
w zależności od tego, czy dany rekord to osoba fizyczna czy podmiot.

Karta obywatela/podmiotu ma pięć zakładek:

- **Dane podstawowe** — dane osobowe/identyfikacyjne, adres, kontakt, adres do e-Doręczeń (ADE),
- **Sprawy** — sprawy, w których dana osoba/podmiot jest stroną,
- **Dokumenty** — dokumenty powiązane z tymi sprawami,
- **Korespondencja** — przesyłki (wychodzące/wpływające) kierowane do tej osoby/podmiotu,
- **Rozrachunki** — należności i wpłaty rozliczane na tę osobę/podmiot.

Oprócz samego przeglądania karty, system udostępnia mechanizm **"przypięcia"** obywatela do
kontekstu aplikacji — raz przypięta osoba jest widoczna i szybko dostępna z dowolnego miejsca w
aplikacji (chip w górnym pasku i skrócona sekcja w menu bocznym), bez konieczności ponownego
wyszukiwania.

## Role

Moduł działa identycznie dla każdej roli — wyszukiwanie, przegląd karty i mechanizm przypięcia nie
są w żaden sposób ograniczone rolą użytkownika.

## Przebieg pracy

### Wyszukiwanie obywatela/podmiotu

W menu bocznym wybierz **Obywatele**. Na liście znajduje się pole wyszukiwania — wpisanie
fragmentu imienia, nazwiska, nazwy podmiotu, numeru PESEL lub numeru NIP zawęża listę na bieżąco,
bez konieczności zatwierdzania frazy. Wyszukiwanie jest częściowe (nie trzeba wpisywać całego
ciągu) i niewrażliwe na wielkość liter.

![Wyszukanie obywatela]({{ '/media/5.%20karta%20obywatelapodmiotu/1.%20wyszukanie%20obywatela.png' | relative_url }})

Lista pokazuje typ rekordu (osoba fizyczna / podmiot), nazwę lub imię i nazwisko, identyfikator
(PESEL lub NIP), adres oraz dane kontaktowe. Kliknięcie wiersza otwiera kartę danej osoby/podmiotu.

### Karta obywatela/podmiotu

Nagłówek karty pokazuje nazwę/imię i nazwisko, typ rekordu oraz identyfikator. Poniżej znajduje się
pięć zakładek.

![Wejście w kartę obywatela]({{ '/media/5.%20karta%20obywatelapodmiotu/2.%20wejście%20w%20kartę%20obywatela.png' | relative_url }})

**Dane podstawowe** (zakładka domyślna) pokazują dane osobowe, adres, dane kontaktowe (e-mail,
telefon) oraz adres do e-Doręczeń (ADE), jeśli jest ustawiony. Zakładka jest czystym podglądem —
nie ma na niej żadnej opcji edycji.

**Sprawy** wyświetla listę spraw, w których ta osoba/podmiot jest stroną — znak sprawy, tytuł,
stan, referenta prowadzącego, datę wszczęcia i termin ustawowy. Kliknięcie wiersza przenosi do
pełnych szczegółów sprawy w module [Sprawy i obiegi](sprawy-i-obiegi.md).

![Sprawy obywatela]({{ '/media/5.%20karta%20obywatelapodmiotu/3.%20sprawy%20obywatela.png' | relative_url }})

**Dokumenty** pokazuje dokumenty powiązane ze sprawami tej osoby/podmiotu — nazwę, typ, sprawę, do
której należą, numer wersji i datę utworzenia. W odróżnieniu od zakładki Sprawy, wiersze tutaj nie
są klikalne — pełen podgląd dokumentu (w tym podpis elektroniczny) jest dostępny tylko przez moduł
[Dokumenty, szablony i podpis elektroniczny](dokumenty-i-podpis.md).

![Dokumenty obywatela]({{ '/media/5.%20karta%20obywatelapodmiotu/4.%20dokumenty%20obywatela.png' | relative_url }})

**Korespondencja** pokazuje przesyłki tej osoby/podmiotu — numer referencyjny, kierunek (wychodząca
/ wpływająca), kanał (np. e-Doręczenia), status oraz daty nadania i ostatniej zmiany statusu.
Podobnie jak w zakładce Dokumenty, wiersze nie prowadzą do szczegółów przesyłki — te są dostępne
przez moduł [Korespondencja i wysyłka pism](korespondencja.md).

![Korespondencja obywatela]({{ '/media/5.%20karta%20obywatelapodmiotu/5.%20korespondencja%20obywatela.png' | relative_url }})

**Rozrachunki** pokazuje sumaryczną kwotę "do zapłaty" oraz dwie listy: należności (z kwotą,
kwotą rozliczoną, terminem płatności i statusem) i wpłat (z kontrahentem, kwotą, datą oraz
informacją, czy wpłata została dopasowana automatycznie). Pełny opis rozliczania płatności znajduje
się w module [Rozliczanie płatności](platnosci.md).

![Rozrachunki obywatela]({{ '/media/5.%20karta%20obywatelapodmiotu/6.%20rozrachunki%20obywatela.png' | relative_url }})

### Przypięcie obywatela do kontekstu aplikacji

Obywatel/podmiot zostaje automatycznie "przypięty" do kontekstu aplikacji w chwili otwarcia jego
karty — wystarczy wejść w wiersz z listy Obywateli. Przypięcie można też wywołać z poziomu karty
szczegółów sprawy w module Sprawy: obok nazwiska/nazwy strony sprawy znajduje się przycisk
**"Przypnij"**, który przypina tę osobę/podmiot bez przechodzenia przez moduł Obywatele.

Po przypięciu:

- w górnym pasku aplikacji pojawia się chip z nazwiskiem/nazwą przypiętej osoby/podmiotu,
- w lewym menu bocznym pojawia się dodatkowa sekcja z nazwą przypiętej osoby, zawierająca pięć
  skróconych pozycji odpowiadających tym samym zakładkom karty (Dane podstawowe, Sprawy,
  Dokumenty, Korespondencja, Rozrachunki) — dzięki temu można przejść do dowolnej zakładki karty
  jednym kliknięciem, bez ponownego wyszukiwania.

Przypięcie jest zapamiętywane w ramach sesji przeglądarki, więc pozostaje aktywne po przejściu
między modułami aplikacji. Otwarcie karty innego obywatela zastępuje poprzednie przypięcie nowym.
Kliknięcie "x" na chipie w górnym pasku usuwa przypięcie — chip i sekcja w menu bocznym znikają.

### Pełny przebieg

Poniższy zapis pokazuje całość opisanego wyżej przebiegu: wyszukanie obywatela, przegląd kolejnych
zakładek karty i przypięcie do kontekstu aplikacji.

![Przebieg pracy z kartą obywatela]({{ '/media/5.%20karta%20obywatelapodmiotu/karta_obywatela.gif' | relative_url }})

## Szczegóły implementacji

Wyszukiwanie obywateli/podmiotów jest realizowane po stronie `document_api` jako dopasowanie
częściowe, niewrażliwe na wielkość liter, po polach: imię, nazwisko, nazwa podmiotu, PESEL, NIP.
Puste zapytanie zwraca pełną listę. System nie waliduje formatu ani sumy kontrolnej numeru PESEL
ani NIP — pola te przyjmują dowolny ciąg znaków wpisany przy rejestracji danych.

## Ograniczenia

Nowego obywatela ani podmiotu nie da się dodać z poziomu aplikacji — ani jako samodzielna akcja w
module Obywatele, ani przy okazji rejestracji nowej sprawy (formularz rejestracji pozwala jedynie
wybrać stronę z już istniejącej listy).

Karta obywatela jest czystym podglądem danych — zakładka "Dane podstawowe" nie ma żadnego
formularza edycji, więc nie da się z poziomu aplikacji zmienić ani uzupełnić danych kontaktowych,
adresu, ani adresu do e-Doręczeń (ADE). W praktyce oznacza to, że osoba lub podmiot, który nie ma
ustawionego adresu ADE, nigdy nie będzie mógł otrzymać korespondencji kanałem e-Doręczeń — zobacz
[Korespondencja i wysyłka pism](korespondencja.md).

Zakładki "Dokumenty" i "Korespondencja" karty obywatela pokazują jedynie listę — nie da się z nich
przejść wprost do pełnych szczegółów dokumentu czy przesyłki (podgląd pliku, podpis, historia
statusu). Żeby zobaczyć te szczegóły, trzeba przejść do odpowiedniego modułu (Dokumenty i szablony
lub Korespondencja) i tam ponownie odnaleźć ten sam rekord.

Przycisk "Akcja zbiorowa" na liście Obywateli jest trwale wyszarzony — dla obywateli nie
zdefiniowano żadnej akcji zbiorowej.
