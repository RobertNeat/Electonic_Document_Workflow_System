---
title: Dokumenty, szablony i podpis elektroniczny
nav_order: 6
---

# Dokumenty, szablony i podpis elektroniczny

## Opis

Moduł **Dokumenty i szablony** zarządza szablonami JasperReports (`.jrxml`) oraz dokumentami
generowanymi z tych szablonów na podstawie danych sprawy. Administrator wgrywa i wersjonuje
szablony — każdy szablon ma przypisany typ dokumentu (jedna z wartości słownika: "Pełnomocnictwo",
"Potwierdzenie opłaty", "Decyzja administracyjna", "Wezwanie do zapłaty") oraz historię wersji z
datą wgrania i autorem. Ponowne wgranie pliku o tej samej nazwie i tym samym typie dokumentu
tworzy kolejną wersję tego szablonu; plik o innej nazwie zakłada nowy szablon od wersji 1, nawet
jeśli ma ten sam typ dokumentu co inny już obecny w systemie.

Z wybranego szablonu i danych wybranej sprawy generowany jest dokument PDF. Każdy dokument ma
własną oś czasu wersji: wersja **niepodpisana** (robocza) powstaje od razu po wygenerowaniu, a
podpisanie certyfikatem nie nadpisuje tej wersji, lecz tworzy kolejną — **podpisaną** (finalną,
niemutowalną). Obie wersje pozostają widoczne na osi czasu ze swoimi własnymi akcjami. Treść PDF
można podejrzeć bezpośrednio w przeglądarce, w dialogu, bez pobierania pliku na dysk.

## Podpis elektroniczny

Podpisanie dokumentu PDF odbywa się w standardzie PAdES, z użyciem certyfikatu użytkownika
wydanego przez demonstracyjną infrastrukturę PKI systemu (własne centrum certyfikacji — root CA i
podrzędne issuing CA). Przy składaniu podpisu serwis odpowiedzialny za podpis odrzuca certyfikat,
który jest nieaktywny lub wygasły, zanim dojdzie do podpisania.

Status podpisu dokumentu przyjmuje jedną z czterech wartości: poprawny, niepoprawny, certyfikat
wygasły albo certyfikat unieważniony. Status jest nadawany dokumentowi w momencie podpisywania —
bezpiecznie, bo złożenie podpisu wymaga wcześniej certyfikatu aktywnego i ważnego, więc dokument
podpisany od razu otrzymuje status "poprawny".

Niezależnie od tego, weryfikacja ważności certyfikatu (np. przy późniejszym sprawdzaniu podpisu)
sprawdza status najpierw przez protokół OCSP, a jeśli ten zawiedzie (błąd sieci, nieautentyczna
odpowiedź, status nieznany) — przez listę CRL jako mechanizm zapasowy; jeżeli żaden z kanałów nie
da wiarygodnej odpowiedzi, wynik jest traktowany jako bezpieczny-zamknięty, czyli "niepoprawny".
Dokument ma też znacznik czasu nadawany przez usługę znakowania czasem (TSA), potwierdzający
moment złożenia podpisu.

Dla podpisanej wersji dostępny jest podgląd szczegółów podpisu: podpisujący (z danymi certyfikatu
w formacie `CN=..., O=..., C=PL`), czas podpisu, odcisk certyfikatu SHA-256, status weryfikacji
OCSP, status weryfikacji CRL oraz znacznik czasu TSA. W obecnej wersji systemu pola statusu OCSP i
CRL w tym podglądzie pokazują wartość "Nieznany" — to rzeczywiste zachowanie usługi weryfikującej,
a nie błąd wyświetlania.

## Role

- **Administrator** — wgrywa nowe szablony Jasper i przegląda ich historię wersji.
- **Referent** — generuje dokumenty z szablonu i danych sprawy, podpisuje dokumenty certyfikatem,
  przegląda listę dokumentów, podgląda PDF oraz szczegóły złożonego podpisu.

## Przebieg pracy

Proces zaczyna się od wgrania szablonu przez administratora w sekcji "Szablony Jasper" w module
Dokumenty i szablony — wskazywany jest typ dokumentu oraz plik `.jrxml`.

![Wgranie szablonu Jasper przez administratora]({{ '/media/2.%20dokumenty%20i%20podpis/1.%20wgranie%20szablonu%20jasper%20przez%20administratora.png' | relative_url }})

Po zatwierdzeniu szablon pojawia się na liście szablonów wraz z numerem wersji, datą wgrania i
autorem; kliknięcie wiersza otwiera historię wersji danego szablonu.

![Szablon dostępny w systemie po wgraniu]({{ '/media/2.%20dokumenty%20i%20podpis/2.%20szablon%20dostępny%20w%20systemie.png' | relative_url }})

Samo wygenerowanie dokumentu z szablonu i danych sprawy nie odbywa się z poziomu formularza
pojedynczego dokumentu, lecz jako akcja zbiorowa w module [Wymiana EDI/XML](wymiana-edi-xml.md), w
zakładce "Akcje zbiorowe", na karcie "Masowe generowanie dokumentów z szablonu". Referent zaznacza
co najmniej jedną sprawę z listy i wybiera szablon z rozwijanej listy (lista pokazuje nazwy plików
szablonów, nie typy dokumentów), po czym uruchamia generowanie — postęp pokazywany jest paskiem aż
do 100% i podsumowaniem liczby przetworzonych dokumentów.

![Wygenerowanie dokumentu na podstawie nowego szablonu przez referenta]({{ '/media/2.%20dokumenty%20i%20podpis/3.%20wygenerowanie%20dokumentu%20na%20pdostawie%20nowego%20szablonu%20przez%20referenta.png' | relative_url }})

Wygenerowany dokument trafia na listę w module Dokumenty i szablony, skąd referent otwiera jego
szczegóły i widzi oś czasu wersji. Na świeżo wygenerowanej (niepodpisanej) wersji dostępny jest
podgląd PDF wprost w dialogu, bez pobierania pliku na dysk.

![Testowy dokument wygenerowany z szablonu, podgląd PDF]({{ '/media/2.%20dokumenty%20i%20podpis/4.%20testowy%20dokument%20wygenerowany.png' | relative_url }})

Na niepodpisanej wersji dokumentu dostępny jest przycisk "Podpisz", który otwiera dialog wyboru
certyfikatu. Certyfikat jest wybieralny tylko wtedy, gdy ma status "aktywny" i jego data wygaśnięcia
jeszcze nie minęła — certyfikaty wygasłe lub unieważnione są wyszarzone i niemożliwe do zaznaczenia,
niezależnie od tego, czy formalna data wygaśnięcia jest jeszcze w przyszłości (dla unieważnionego
certyfikatu liczy się wyłącznie status, nie data).

![Podpisanie dokumentu wybranym certyfikatem]({{ '/media/2.%20dokumenty%20i%20podpis/5.%20podpisanie%20dokumentu%20certyfikatem.png' | relative_url }})

Po podpisaniu na osi czasu pojawia się nowa wersja dokumentu ze statusem podpisu (np. "Poprawny"),
z przyciskiem "Szczegóły podpisu" zamiast "Podpisz". Wcześniejsza, niepodpisana wersja pozostaje na
osi czasu niezmieniona, oznaczona jako wersja robocza — dzięki czemu obie wersje, robocza i
finalna, są zawsze dostępne do przeglądu. Dialog szczegółów podpisu pokazuje dane podpisującego,
czas podpisu, odcisk certyfikatu SHA-256, status OCSP, status CRL i znacznik czasu TSA.

![Dokument podpisany certyfikatem, szczegóły podpisu elektronicznego]({{ '/media/2.%20dokumenty%20i%20podpis/6.%20dokument%20podpisany%20certyfikatem.png' | relative_url }})

Poniżej pełny przebieg podpisywania w akcji.

![Pełny przebieg podpisywania dokumentu]({{ '/media/2.%20dokumenty%20i%20podpis/złożenie_podpisu.gif' | relative_url }})

## Szczegóły implementacji

`jasper_service` jest bezstanowym silnikiem renderującym szablony `.jrxml` do PDF. Wymaga formatu
zgodnego z JasperReports 7.x — loader oparty o Jackson, nie starego schematu Digester/XSD z serii
5/6 (różnice obejmują m.in. brak namespace na elemencie głównym oraz inną strukturę bandów i
elementów wizualnych). Dialog wgrywania szablonu waliduje wyłącznie rozszerzenie pliku (`.jrxml`) —
plik w starym formacie przejdzie upload bez błędu, ale zostanie odrzucony dopiero przy próbie
wygenerowania z niego dokumentu.

`sign_service` wykonuje podpisywanie i weryfikację z użyciem biblioteki EU DSS, na własnej
dwupoziomowej hierarchii PKI (root CA oraz podrzędne issuing CA) wraz z własnym responderem
OCSP i wystawcą listy CRL dla certyfikatów wydanych przez issuing CA.

`dms_service` przechowuje treść plików (szablonów i wygenerowanych dokumentów) na dedykowanym
wolumenie, odrębnym od pozostałych serwisów — sam przechowuje jedynie metadane w swojej bazie.

## Ograniczenia

Certyfikatów nie da się unieważnić ani wygenerować nowych z poziomu aplikacji — dostępne są
wyłącznie te, które istnieją w danych demonstracyjnych od startu systemu. W konsekwencji nie da się
w trakcie korzystania z aplikacji doprowadzić świeżo podpisanego dokumentu do statusu "certyfikat
wygasły" lub "certyfikat unieważniony" — te statusy można zobaczyć wyłącznie na dokumentach
podpisanych wcześniej, zanim odpowiedni certyfikat wygasł albo został unieważniony.

Przycisk "Wygeneruj wezwanie" w module [Rozliczanie płatności](platnosci.md) nie generuje realnego
dokumentu — jest to pusty dialog potwierdzenia. Wezwanie do zapłaty jako faktyczny dokument da się
wygenerować wyłącznie przez akcję zbiorową "Masowe generowanie dokumentów z szablonu" w module
Wymiana EDI/XML, z dowolnym szablonem — nie jednym dedykowanym kliknięciem przy konkretnej
należności.

Podgląd samej treści pliku `.jrxml` szablonu nie jest dostępny z poziomu aplikacji — widoczne są
tylko metadane i historia wersji.

Podpis dokumentów i komunikatów XML w standardzie XAdES nie jest wykorzystywany end-to-end w
aplikacji — jedynym używanym rodzajem podpisu jest podpis PDF w standardzie PAdES.

Pola statusu OCSP i statusu CRL w dialogu szczegółów podpisu pokazują obecnie wartość "Nieznany" —
to rzeczywisty wynik zwracany przez usługę weryfikującą, nie usterka interfejsu; mechanizm
weryfikacji po stronie serwera (najpierw OCSP, z CRL jako mechanizmem zapasowym) działa, ale jego
wynik nie jest odzwierciedlony w tych dwóch szczegółowych polach odpowiedzi.

Lista certyfikatów w dialogu wyboru certyfikatu do podpisu obejmuje wszystkie certyfikaty
demonstracyjne obecne w systemie, bez powiązania między zalogowanym użytkownikiem a certyfikatem —
to referent decyduje, którym certyfikatem podpisać dokument.
