# Przepływ: Wymiana EDI/XML i akcje zbiorowe

## Zakres

Scenariusz testowy do manualnego przejścia: import komunikatu sprawy XML, import faktury kosztowej
UBL, oraz akcje zbiorowe na sprawach (zmiana stanu, wysyłka pism, generowanie dokumentów).
Aplikacja: `document_web` (`http://localhost:10600`).

## Aktorzy i role

| Login w Keycloaku | Rola       | Kiedy potrzebny                                 |
| ----------------- | ---------- | ----------------------------------------------- |
| `demo.referent`   | referent   | import XML, akcje zbiorowe, import UBL          |
| `demo.ksiegowosc` | ksiegowosc | alternatywnie do importu UBL (faktury kosztowe) |

Hasło obu kont: wspólne dla kont demo, zmienna `KEYCLOAK_DEMO_USER_PASSWORD` (domyślnie
`change-me`).

## Kroki przepływu

1. Zaloguj się jako `demo.referent`. Przejdź do menu **Wymiana EDI / XML**.
2. Zobaczysz dwie zakładki: **"Wymiana XML (EZD)"** i **"Wymiana UBL/PEPPOL"**, każda z historią
   importów.
3. **Import komunikatu XML** — na zakładce "Wymiana XML (EZD)" kliknij **"Nowy import"**, wybierz
   plik `.xml` (zob. "Dane testowe") i prześlij. Wynik walidacji i podgląd zmapowanych pól pojawi
   się na tej samej stronie — komunikat "sukces=false" to normalny wynik walidacji, nie błąd
   aplikacji.
4. **Import faktury UBL** — na zakładce "Wymiana UBL/PEPPOL" kliknij **"Nowy import"**, wybierz
   plik faktury UBL i prześlij. Zobaczysz podgląd zmapowanej faktury: kontrahent, NIP, kwota,
   pozycje.
5. **Akcje zbiorowe** — ten ekran nie jest dostępny z żadnego menu. Wpisz w przeglądarce adres
   `http://localhost:10600/wymiana/akcje-zbiorowe` (zob. "Znane ograniczenia", pkt 1).
6. Na ekranie "Akcje zbiorowe" są trzy karty:
   - **"Masowa zmiana stanu spraw"** — zaznacz checkboxem jedną lub więcej spraw, wybierz nowy
     stan z listy, kliknij **"Uruchom zmianę stanu"**.
   - **"Masowa wysyłka pism"** — zaznacz sprawy, wybierz kanał wysyłki, kliknij
     **"Uruchom wysyłkę"**.
   - **"Masowe generowanie dokumentów z szablonu"** — zaznacz sprawy, wybierz szablon, kliknij
     **"Uruchom generowanie"**.
7. Po uruchomieniu każdej z akcji pod przyciskiem pojawi się pasek postępu, aktualizowany
   automatycznie (co ok. 1-2 s) do zakończenia operacji. Wyniki błędne dla poszczególnych spraw są
   wypisane poniżej paska.

## Dane testowe do manualnego przejścia przepływu

### Komunikat sprawy XML (krok 3)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<KomunikatSprawy>
    <znakSprawy>WO.6220.14.2026</znakSprawy>
    <tytul>Decyzja o środowiskowych uwarunkowaniach dla budowy obwodnicy</tytul>
    <jrwa>6220</jrwa>
    <wydzial>Wydział Ochrony Środowiska</wydzial>
    <obywatelId>ob-42</obywatelId>
    <terminUstawowy>2026-12-01</terminUstawowy>
</KomunikatSprawy>
```

Gotowe pliki testowe w repozytorium: `apps/document_api/src/test/resources/wymiana/
komunikat-sprawy-poprawny.xml` (poprawny) oraz `komunikat-sprawy-niepoprawny.xml` (do testu
ścieżki błędu — błędy walidacji wyświetlą się w tabeli na ekranie).

### Faktura UBL/PEPPOL (krok 4)

Gotowy plik testowy: `apps/document_api/src/test/resources/wymiana/faktura-ubl-poprawna.xml`
(kontrahent "Elektromontaż Mazowsze Sp. z o.o.", kwota 7380,50 PLN). Pliki do testu ścieżki błędu:
`faktura-ubl-brak-pol.xml` (brakujące pola) i `faktura-ubl-niepoprawny-namespace.xml` (błędny
namespace) — oba zwrócą listę błędów walidacji widoczną w interfejsie.

### Akcje zbiorowe (kroki 5-7)

Wystarczy wybrać istniejące sprawy w tabeli — np. `WO.6220.14.2026`, `WO.6220.21.2026`,
`WO.6220.23.2026` (wszystkie trzy w wydziale Ochrony Środowiska, stan "W toku"), bez potrzeby
przygotowywania żadnych plików.

## Znane ograniczenia (czego nie da się zrobić w aplikacji)

1. **Ekran "Akcje zbiorowe" nie jest dostępny z żadnego menu, przycisku czy linku w
   aplikacji.** Trzeba wpisać adres `http://localhost:10600/wymiana/akcje-zbiorowe` ręcznie w
   przeglądarce. Lista Obywateli ma swój osobny przycisk "Akcja zbiorowa" — jest on jednak trwale
   wyszarzony i niezwiązany z tym ekranem.
2. **Nie da się wysłać (eksportować) komunikatu XML do systemu zewnętrznego** — tylko odbieranie
   (import) jest zaimplementowane. Brak przycisku eksportu w całym module Wymiana.
3. **Akcje zbiorowe obsługują wyłącznie te trzy typy operacji** widoczne na ekranie — żadnych
   innych.
