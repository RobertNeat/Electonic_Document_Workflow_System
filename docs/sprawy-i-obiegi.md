---
title: Sprawy i obiegi
nav_order: 5
---

# Sprawy i obiegi

## Opis

Moduł Sprawy to rejestr spraw administracyjnych prowadzonych przez urząd — każda sprawa jest
przypisana do jednej **definicji obiegu**, czyli konfigurowalnego opisu procesu: zestawu
dozwolonych stanów, przejść między nimi oraz ról i typów dokumentów wymaganych na poszczególnych
przejściach. System przewiduje kilka rodzajów obiegów (np. obieg wniosku o wydanie decyzji
administracyjnej, obieg skargi/wniosku, obieg faktury kosztowej, obieg wewnętrznego pisma
okólnego) — każdy z nich ma własną sekwencję stanów dopasowaną do charakteru sprawy.

Przy rejestracji sprawa otrzymuje automatycznie **znak sprawy** zbudowany zgodnie z JRWA
(Jednolity Rzeczowy Wykaz Akt): symbol wydziału, pozycja JRWA, numer kolejny w ramach
wydziału/pozycji/roku oraz rok. Pozycja JRWA niesie też kategorię archiwalną (A albo B wraz z
okresem przechowywania), która uzupełnia się na formularzu automatycznie i nie jest edytowalna
ręcznie.

Każda sprawa ma swoją **metrykę** — chronologiczną historię czynności (rejestracja, każda zmiana
stanu z datą i osobą wykonującą), widoczną na ekranie szczegółów sprawy i aktualizującą się
automatycznie po każdym przejściu.

## Role

W obiegu sprawy uczestniczą dwie role:

- **referent** — rejestruje nowe sprawy i wykonuje przejścia stanu, które nie wymagają szczególnej
  roli (np. "Zarejestrowana → W toku", "W toku → Do akceptacji").
- **kierownik** — wymagany na przejściach oznaczonych w definicji obiegu jako zastrzeżone dla tej
  roli, np. zatwierdzenie sprawy przejściem "Do akceptacji → Zakończona" w obiegu wniosku o wydanie
  decyzji administracyjnej. Dopóki sprawa czeka na takie przejście, może je wykonać wyłącznie
  użytkownik z rolą kierownika — referent widzi przycisk przejścia, ale jest on nieaktywny wraz z
  komunikatem o brakującym wymogu.

## Przebieg pracy

Rejestracja nowej sprawy odbywa się z poziomu listy spraw, która udostępnia wyszukiwanie i filtry
(stan, wydział, treść). Formularz rejestracji wymaga wyboru definicji obiegu, pozycji JRWA, tytułu
sprawy, obywatela/podmiotu (z istniejącej listy), wydziału oraz referenta (domyślnie użytkownik
zalogowany, edytowalny) — opcjonalnie można też podać termin ustawowy. Kategoria archiwalna
uzupełnia się automatycznie po wyborze pozycji JRWA.

![Rejestracja nowej sprawy]({{ '/media/1.%20przeprowadzenie%20sprawy/1.%20rejestracja%20nowej%20sprawy.png' | relative_url }})

Po zapisaniu system nadaje znak sprawy i przechodzi do ekranu szczegółów, na którym widać dane
podstawowe, kartę "Obieg sprawy" z paskiem wszystkich stanów danej definicji oraz kartę "Metryka
sprawy". Sprawa startuje w pierwszym stanie obiegu — dla obiegu wniosku o wydanie decyzji
administracyjnej jest to stan "Zarejestrowana".

![Szczegóły sprawy w stanie Zarejestrowana]({{ '/media/1.%20przeprowadzenie%20sprawy/2.%20szczegóły%20sprawy%20-%20krok%201%20zarejestrowana.png' | relative_url }})

Karta "Obieg sprawy" pokazuje dla aktualnego stanu przycisk "Przejdź do: {nazwa następnego stanu}"
dla każdego dozwolonego przejścia. Jeśli przejście nie wymaga żadnej dodatkowej roli ani
dokumentu, przycisk jest od razu aktywny i jego użycie przenosi sprawę do kolejnego stanu — tak
jak w przejściu "Zarejestrowana → W toku".

![Szczegóły sprawy w stanie W toku]({{ '/media/1.%20przeprowadzenie%20sprawy/2.%20szczegóły%20sprawy%20-%20krok%202%20w%20toku.png' | relative_url }})

Przykładowy pełny obieg "Obieg wniosku o wydanie decyzji administracyjnej" przechodzi przez stany
Zarejestrowana → W toku → Do akceptacji → Zakończona → Zamknięta. Po osiągnięciu stanu
"Do akceptacji" przycisk przejścia do stanu "Zakończona" jest widoczny, ale zablokowany — pod nim
pojawia się informacja o niespełnionych wymaganiach, tutaj: rola kierownika i dokument typu
"Decyzja administracyjna".

![Szczegóły sprawy w stanie Do akceptacji, przejście zablokowane]({{ '/media/1.%20przeprowadzenie%20sprawy/2.%20szczegóły%20sprawy%20-%20krok%203%20do%20akceptacji.png' | relative_url }})

Dokument wymaganego typu nie dołącza się z poziomu ekranu sprawy — tę operację wykonuje się w
module [Wymiana EDI/XML](wymiana-edi-xml.md), w zakładce "Akcje zbiorowe", karcie "Masowe
generowanie dokumentów z szablonu". Zaznaczenie sprawy na liście, wybór odpowiedniego szablonu
(wygenerowany dokument dziedziczy typ dokumentu po szablonie) i uruchomienie generowania tworzy
nową wersję dokumentu powiązaną ze sprawą; pasek postępu pokazuje stan przetwarzania aż do 100%.

![Masowe generowanie dokumentu z szablonu w module Wymiana EDI/XML]({{ '/media/1.%20przeprowadzenie%20sprawy/2.%20szczegóły%20sprawy%20-%20krok%203%20.2%20wygenerowanie%20dokumentu%20z%20szablonu.png' | relative_url }})

Po wygenerowaniu dokumentu wymóg dokumentu na przejściu jest spełniony — pozostaje tylko wymóg
roli. Komunikat pod zablokowanym przyciskiem zawęża się wtedy do brakującej roli kierownika.

![Szczegóły sprawy, pozostał tylko wymóg roli kierownika]({{ '/media/1.%20przeprowadzenie%20sprawy/2.%20szczegóły%20sprawy%20-%20krok%203.3%20wymagane%20zatwierdzenie%20kierownika.png' | relative_url }})

Gdy ten sam ekran szczegółów otworzy użytkownik z rolą kierownika, przycisk przejścia "Do
akceptacji → Zakończona" jest już aktywny — zatwierdzenie sprawy wykonuje się tym przejściem i
system odnotowuje je w metryce.

![Zatwierdzenie sprawy przez kierownika, stan Zakończona]({{ '/media/1.%20przeprowadzenie%20sprawy/3.%20szczegóły%20sprawy%20-%20krok%204%20zatwierdzenie%20przez%20kierownika.png' | relative_url }})

Ostatnie przejście w tym obiegu, "Zakończona → Zamknięta", nie wymaga żadnej roli ani dokumentu i
może je wykonać zarówno referent, jak i kierownik. Po osiągnięciu stanu "Zamknięta" — ostatniego w
sekwencji — karta obiegu informuje, że sprawa znajduje się w ostatnim stanie i nie proponuje już
kolejnych przejść.

![Zamknięcie sprawy, stan Zamknięta]({{ '/media/1.%20przeprowadzenie%20sprawy/4.%20szczegóły%20sprawy%20-%20krok%205%20zamknięcie%20sprawy.png' | relative_url }})

Wygenerowany w toku sprawy dokument można podejrzeć bez pobierania pliku — podgląd PDF otwiera się
w module Dokumenty i szablony i pokazuje treść zbudowaną z danych sprawy i obywatela.

![Podgląd wygenerowanego dokumentu "Decyzja o środowiskowych uwarunkowaniach"]({{ '/media/1.%20przeprowadzenie%20sprawy/5.%20podgląd%20dokumentu%20wygenerowanego%20w%20toku%20sprawy.png' | relative_url }})

Obok tego obiegu system udostępnia też prostsze obiegi, np. "Obieg skargi / wniosku" — jego sekwencja
stanów (Zarejestrowana → W toku → Odpowiedź przygotowana → Zakończona) nie zawiera żadnego
przejścia wymagającego konkretnej roli ani dołączonego dokumentu, więc referent może przeprowadzić
taką sprawę od rejestracji do zakończenia samodzielnie.

Poniższa animacja pokazuje pełny przebieg w akcji — od rejestracji sprawy, przez generowanie
wymaganego dokumentu, zatwierdzenie przez kierownika, aż po zamknięcie sprawy.

![Pełny przebieg przeprowadzenia sprawy]({{ '/media/1.%20przeprowadzenie%20sprawy/przeprowadzenie_sprawy.gif' | relative_url }})

## Szczegóły implementacji

Definicje obiegów (stany, przejścia, wymagane role i typy dokumentów na przejściu) są danymi
konfiguracyjnymi przechowywanymi w schemie `core` bazy danych `document_api`, w osobnych tabelach
`definicja_obiegu` i `przejscie_obiegu`, a nie kodem — nowy rodzaj sprawy wymaga tylko nowej
definicji wraz z jej przejściami, bez zmian w logice aplikacji. Maszyna stanów działa w całości
wewnątrz `document_api` (`SprawaService.zmienStan`), bez zewnętrznego silnika workflow: przy próbie
zmiany stanu
serwis sprawdza, czy istnieje przejście z aktualnego stanu do żądanego w ramach definicji obiegu
przypisanej do sprawy, a następnie czy wołający spełnia wymaganą rolę i czy sprawa ma dołączony
dokument każdego wymaganego typu — brak któregokolwiek z tych warunków blokuje przejście po
stronie serwera, niezależnie od tego, czy przycisk na froncie był aktywny. Po przejściu do
ostatniego stanu w sekwencji danej definicji sprawa oznaczana jest jako zamknięta, a każda zmiana
stanu dopisuje wpis do metryki sprawy.

## Ograniczenia

Z poziomu aplikacji nie da się utworzyć nowej definicji obiegu — ekran "Definicje obiegów" w
Administracji pozwala zmienić tylko nazwę i opis istniejącego obiegu, natomiast jego stany,
przejścia, wymagane role i typy dokumentów pozostają niezmienne i nie są edytowalne z interfejsu.

Dokumentu wymaganego na danym przejściu nie da się dołączyć do sprawy bezpośrednio z ekranu jej
szczegółów — jedyną drogą jest zakładka "Akcje zbiorowe" w module Wymiana EDI/XML, a sam ekran
szczegółów sprawy nie podpowiada tej ścieżki, mimo że to tam widać, że przejście jest zablokowane
brakiem dokumentu.

Lista wydziałów dostępnych przy rejestracji sprawy jest zawsze taka sama (stały zestaw czterech
nazw) i nie można jej rozszerzyć z poziomu aplikacji.

Formularz rejestracji sprawy nie pozwala dodać nowego obywatela/podmiotu w trakcie wypełniania —
można wybrać wyłącznie podmiot już istniejący w rejestrze obywateli.

Dekretacja dokumentu przychodzącego, rozumiana jako samodzielne przydzielenie go do
wydziału/referenta jeszcze przed rejestracją sprawy, nie istnieje jako osobna funkcja — pola
Wydział i Referent wypełnia się tylko raz, w momencie rejestracji sprawy.
