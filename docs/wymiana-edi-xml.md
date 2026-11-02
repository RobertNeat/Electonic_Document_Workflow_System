---
title: Wymiana EDI/XML i akcje zbiorowe
nav_order: 10
---

# Wymiana EDI/XML i akcje zbiorowe

## Opis

Moduł **Wymiana EDI/XML** łączy trzy niezależne funkcje w jednym ekranie (`document_web`,
`/wymiana`):

- import komunikatów sprawy w formacie XML, zgodnie z zasadami wymiany dokumentacji między
  systemami klasy EZD,
- import faktur kosztowych w formacie UBL/PEPPOL (elektroniczna wymiana dokumentów handlowych,
  EDI),
- akcje zbiorowe — operacje wykonywane jednocześnie na wielu sprawach naraz.

Wszystkie trzy funkcje są dostępne jako zakładki tej samej strony, pod wspólną pozycją menu
**Wymiana EDI / XML**.

## Role

- **referent** — ma dostęp do importu XML i do akcji zbiorowych; może również korzystać z importu
  UBL.
- **ksiegowosc** — alternatywna rola do importu faktur UBL (faktury kosztowe).

## Import XML (EZD)

Na zakładce **"Wymiana XML (EZD)"** użytkownik przesyła plik `.xml` zawierający komunikat sprawy.
Element główny dokumentu musi mieć nazwę `<KomunikatSprawy>` — inna nazwa elementu głównego
powoduje błąd walidacji. Wewnątrz komunikatu wymagane i opcjonalne są następujące pola:

- `znakSprawy` — wymagane, tekst.
- `tytul` — wymagane, tekst.
- `jrwa` — wymagane, tekst. Nie jest sprawdzane, czy podany symbol istnieje w słowniku JRWA —
  akceptowany jest dowolny ciąg znaków.
- `wydzial` — wymagane, tekst. Nie jest sprawdzane przeciw liście wydziałów zdefiniowanych w
  systemie — akceptowany jest dowolny ciąg znaków.
- `obywatelId` — wymagane, tekst. Na etapie walidacji XML nie jest sprawdzane, czy taki obywatel
  istnieje w bazie.
- `terminUstawowy` — opcjonalne, data. Jeśli pole jest obecne, musi być zapisane w formacie
  ISO-8601 (`rrrr-mm-dd`), inaczej walidacja zwraca błąd "Nieprawidłowy format daty".

Przykład poprawnego komunikatu:

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

Po przesłaniu pliku wynik walidacji oraz podgląd zmapowanych pól pojawiają się na tej samej
stronie. Odpowiedź z informacją "sukces=false" jest normalnym wynikiem walidacji semantycznej
(dane w pliku nie przeszły kontroli), a nie błędem aplikacji.

## Import UBL/PEPPOL

Na zakładce **"Wymiana UBL/PEPPOL"** użytkownik przesyła plik `.xml` z fakturą kosztową w formacie
UBL 2.1. Walidacja jest realizowana na poziomie struktury DOM (bez formalnego schematu XSD) i
sprawdza:

- namespace elementu głównego — musi być `urn:oasis:names:specification:ubl:schema:xsd:Invoice-2`,
  a sam element główny musi nazywać się `Invoice`; inny namespace powoduje błąd "Nieprawidłowa
  przestrzeń nazw UBL",
- numer faktury (`cbc:ID`) — wymagany,
- nazwę kontrahenta (`PartyLegalEntity/RegistrationName`, z fallbackiem na
  `PartyName/Name`) — wymagana,
- NIP kontrahenta (`PartyLegalEntity/CompanyID`, z fallbackiem na `PartyTaxScheme/CompanyID`) —
  wymagany, przy czym sprawdzana jest tylko jego obecność — brak walidacji formatu lub sumy
  kontrolnej NIP,
- kwotę do zapłaty (`LegalMonetaryTotal/PayableAmount`) — wymagana, musi parsować się jako wartość
  dziesiętna, inaczej walidacja zwraca błąd "Brak lub nieprawidłowa wartość kwoty".

Po przesłaniu pliku widoczny jest podgląd zmapowanej faktury: kontrahent, NIP, kwota, pozycje.
Import ma charakter wyłącznie podglądowy — zwraca mapowanie danych i proponowany numer
referencyjny, ale **nie zapisuje żadnej należności w bazie**. Z tego powodu zaimportowana faktura
nie koliduje z należnościami zarejestrowanymi niezależnie w module rozliczania płatności, nawet
jeśli dotyczą tego samego kontrahenta i tej samej kwoty.

## Akcje zbiorowe

Zakładka **"Akcje zbiorowe"** udostępnia trzy karty, każda obsługująca inny typ operacji na wielu
sprawach jednocześnie:

- **Masowa zmiana stanu spraw** — zaznaczenie co najmniej jednej sprawy i wybranie nowego stanu z
  listy, po czym uruchomienie zmiany dla wszystkich zaznaczonych spraw naraz.
- **Masowa wysyłka pism** — zaznaczenie spraw i wybranie kanału wysyłki, a następnie uruchomienie
  wysyłki. Szczegóły samego mechanizmu wysyłki opisane są w
  [Korespondencja i wysyłka pism](korespondencja.md).
- **Masowe generowanie dokumentów z szablonu** — zaznaczenie spraw i wybranie szablonu, a następnie
  uruchomienie generowania dokumentów dla wszystkich zaznaczonych spraw. Szczegóły generowania i
  podpisu dokumentów opisane są w
  [Dokumenty, szablony i podpis elektroniczny](dokumenty-i-podpis.md).

Każda z trzech operacji jest przetwarzana asynchronicznie — po uruchomieniu akcji pod przyciskiem
pojawia się pasek postępu, aktualizowany automatycznie aż do zakończenia operacji. Błędy
dotyczące poszczególnych spraw są wypisywane na liście wyników pod paskiem postępu.

Zakładka "Akcje zbiorowe" jest widoczna normalnie, razem z zakładkami "Wymiana XML (EZD)" i
"Wymiana UBL/PEPPOL", na tej samej stronie modułu Wymiana EDI/XML (menu **Wymiana EDI / XML**) —
nie wymaga wpisywania żadnego dodatkowego adresu URL.

## Szczegóły implementacji

Uruchomienie akcji zbiorowej publikuje żądanie do kolejki RabbitMQ, a `document_api` odpowiada
natychmiast kodem **202** (żądanie przyjęte do przetworzenia). Workery konsumujące komunikaty z
kolejki wykonują operację pojedynczo dla każdej zaznaczonej sprawy i raportują zbiorczy postęp
oraz wynik. Front odpytuje endpoint statusu akcji pollingiem co 1-2 sekundy; odpytywanie jest
zatrzymywane automatycznie po zakończeniu operacji.

## Ograniczenia

Eksport komunikatów XML do systemu zewnętrznego nie jest zaimplementowany — moduł obsługuje
wyłącznie odbiór (import) komunikatów sprawy, bez możliwości ich nadania w drugą stronę.

Lista Obywateli ma własny, osobny przycisk "Akcja zbiorowa" — jest on jednak trwale wyszarzony i
niezwiązany z zakładką "Akcje zbiorowe" w module Wymiana EDI/XML.

Akcje zbiorowe obsługują wyłącznie trzy typy operacji opisane powyżej (zmiana stanu spraw, wysyłka
pism, generowanie dokumentów z szablonu) — żadnych innych operacji zbiorczych nie da się w ten
sposób wykonać.
