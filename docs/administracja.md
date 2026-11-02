---
title: Administracja
nav_order: 11
---

# Administracja

## Opis

Panel Administracja jest widoczny w menu bocznym `document_web` wyłącznie dla roli
`administrator`. Panel składa się z pięciu kart:

- **Użytkownicy i role** — lista profili domenowych, dodawanie nowych użytkowników, zmiana ról
  (pojedynczo lub zbiorczo).
- **Struktura organizacyjna** — podgląd drzewa wydział → stanowiska.
- **Słowniki** — cztery zakładki (JRWA, Typy dokumentów, Dostawcy wysyłki, Stawki opłat) z edycją
  istniejących pozycji.
- **Definicje obiegów** — podgląd sekwencji stanów i przejść, edycja nazwy i opisu obiegu.
- **Dziennik zdarzeń** — przegląd i filtrowanie dziennika operacji wykonanych w systemie.

## Role

Pozycja menu **Administracja** w menu bocznym jest widoczna tylko dla roli `administrator` —
pozostałe role (`referent`, `kierownik`, `ksiegowosc`) nie mają tego punktu wejścia w menu, więc w
praktyce nie korzystają z żadnej z pięciu kart panelu. Karta **Dziennik zdarzeń** jest wyjątkiem pod
względem funkcjonalnym: sam ekran nie ma żadnych ograniczeń zależnych od roli (nie sprawdza, kto go
otworzył) i działa identycznie dla każdej zalogowanej roli — w odróżnieniu od karty Słowniki, gdzie
przyciski edycji są ukrywane dla ról innych niż administrator.

Mechanizm logowania przez Keycloak oraz pełna lista ról i kont demo jest opisana na stronie
[Logowanie, role i konta demo](logowanie-i-role.md).

## Użytkownicy i role

Karta prezentuje listę profili domenowych: login, imię i nazwisko, wydział, stanowisko oraz role.
Profile te są niezależne od kont logowania w Keycloaku — to odrębny zbiór danych.

**Dodawanie nowego użytkownika** odbywa się przez dialog z polami:

- Login, imię, nazwisko — pola tekstowe, wymagane, sprawdzana jest tylko niepustość.
- E-mail — pole tekstowe, wymagane, jedyne pole w dialogu z walidacją formatu adresu e-mail.
- Wydział — lista wyboru zasilana danymi ze struktury organizacyjnej, wymagana.
- Stanowisko — pole tekstowe, wymagane.
- Role domenowe — wielokrotny wybór z wartości `administrator`, `kancelaria`, `referent`,
  `kierownik`, `ksiegowosc`; pole opcjonalne przy tworzeniu — nowy profil można zapisać bez żadnej
  roli.

**Zmiana ról istniejącego użytkownika** odbywa się bezpośrednio w wierszu listy (zmiana zapisuje
się natychmiast) i w tym przypadku lista ról nie może być pusta — wymagana jest co najmniej jedna
rola. Zmianę można też wykonać zbiorczo: po zaznaczeniu kilku wierszy checkboxem dialog "Zarządzaj
rolami" pozwala nadać tę samą rolę kilku profilom naraz.

**Ważne zastrzeżenie:** zmiana roli w tym ekranie modyfikuje wyłącznie profil domenowy w
`document_web` — nie wpływa na faktyczne uprawnienia użytkownika w systemie. O tym, co dany
użytkownik może rzeczywiście zrobić, decyduje wyłącznie rola przypisana jego kontu w Keycloaku.
Profile domenowe i konta logowania w Keycloaku to dwa niezsynchronizowane zbiory danych — więcej na
stronie [Logowanie, role i konta demo](logowanie-i-role.md).

## Struktura organizacyjna

Karta prezentuje drzewo organizacyjne: wydział → stanowiska, wraz z kierownikiem przypisanym do
każdego wydziału. Jest to wyłącznie podgląd — ekran nie udostępnia żadnej funkcji edycji, niezależnie
od roli zalogowanego użytkownika.

## Słowniki

Karta zawiera cztery zakładki: **JRWA**, **Typy dokumentów**, **Dostawcy wysyłki**, **Stawki
opłat**. Na każdej zakładce dostępna jest edycja istniejącego wiersza (ikona edycji otwiera dialog
ze zmianą wartości pozycji). Przyciski edycji są widoczne wyłącznie dla roli administrator —
niezależnie od sposobu wejścia na ekran, dla innych ról te same listy są wyświetlane bez przycisków
edycji. Zakres edycji jest ograniczony do istniejących pozycji; ekran nie udostępnia dodawania
nowych wierszy w żadnym z czterech słowników.

## Definicje obiegów

Karta prezentuje listę definicji obiegów spraw. Podgląd stanów pozwala zobaczyć sekwencję stanów i
przejść danego obiegu w dialogu. Dla administratora dostępna jest edycja ograniczona do nazwy i
opisu definicji obiegu — stany, przejścia oraz role i dokumenty wymagane na poszczególnych etapach
nie są edytowalne z tego ekranu. Szczegóły działania samych stanów i przejść obiegu opisuje strona
[Sprawy i obiegi](sprawy-i-obiegi.md).

## Dziennik zdarzeń

Karta pozwala przeglądać dziennik zdarzeń (audit log) operacji wykonywanych w systemie, z
możliwością filtrowania po typie operacji (np. rejestracja sprawy, zmiana stanu sprawy, podpisanie
dokumentu, rejestracja wpłaty, wysyłka pisma, dekretacja, zmiana danych użytkownika, wygenerowanie
wezwania do zapłaty) oraz po zakresie dat. Filtry można wyczyścić jednym przyciskiem. Ekran nie
ogranicza niczego w zależności od roli — działa tak samo dla każdej zalogowanej roli, w
odróżnieniu od pozostałych czterech kart, których widoczność w menu i możliwość edycji są
zarezerwowane dla administratora.

## Ograniczenia

Zmiana roli w ekranie "Użytkownicy i role" nie wpływa na faktyczne uprawnienia użytkownika w
systemie — o tym decyduje wyłącznie rola przypisana jego kontu w Keycloaku, a profile domenowe w
`document_web` i konta logowania w Keycloaku są dwoma niezsynchronizowanymi zbiorami danych: dodanie
profilu w tym ekranie nie tworzy konta logowania w Keycloaku i odwrotnie.

Struktura organizacyjna jest tylko podglądem — nie da się dodać nowego wydziału ani stanowiska, ani
zmienić kierownika wydziału.

Słowniki można jedynie edytować wiersz po wierszu — nie da się dodać nowej pozycji w żadnym z
czterech słowników (JRWA, Typy dokumentów, Dostawcy wysyłki, Stawki opłat).

Nie da się utworzyć nowej definicji obiegu ani zmienić jej stanów, przejść, wymaganych ról czy
wymaganych dokumentów — dostępna edycja ogranicza się do nazwy i opisu. Wprowadzenie nowego rodzaju
sprawy wymaga działania poza aplikacją.

Nie da się edytować profilu użytkownika po jego utworzeniu, poza listą ról — zmiana wydziału,
stanowiska, imienia, nazwiska czy adresu e-mail nie jest dostępna z tego ekranu. Nie da się też
usunąć ani dezaktywować istniejącego profilu.
