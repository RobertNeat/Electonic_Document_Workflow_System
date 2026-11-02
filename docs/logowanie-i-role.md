---
title: Logowanie, role i konta demo
nav_order: 3
---

# Logowanie, role i konta demo

## Uwierzytelnianie

Logowanie odbywa się przez Keycloak (OIDC Authorization Code + PKCE). Po wejściu na
niezalogowaną stronę `document_web` aplikacja przekierowuje na ekran logowania Keycloaka — nie ma
własnego formularza login/hasło we frontendzie. Po zalogowaniu użytkownik trafia na **Pulpit**,
czyli domyślną stronę startową.

O tym, co dany użytkownik faktycznie może zrobić w systemie, decyduje **wyłącznie rola przypisana
jego kontu w Keycloaku** — nie dane w ekranie "Użytkownicy i role" modułu Administracja. Te dwa
zbiory danych (profile domenowe w `document_web` i konta logowania w Keycloaku) nie są
zsynchronizowane.

## Role

| Rola | Opis | Widoczne moduły / uprawnienia specjalne |
| --- | --- | --- |
| `administrator` | administrator systemu | jedyna rola widząca menu **Administracja**; wgrywanie szablonów Jasper; edycja słowników i definicji obiegów |
| `referent` | referent prowadzący sprawy | rejestracja i prowadzenie spraw, generowanie i podpisywanie dokumentów, nadawanie korespondencji |
| `kierownik` | kierownik wydziału | jak referent, plus zatwierdzanie przejść obiegu wymagających roli kierownika (np. "Do akceptacji → Zakończona") |
| `ksiegowosc` | księgowość | moduł Płatności — rejestr należności/wpłat, import wyciągów bankowych, rekoncyliacja |

## Konta demo

Cztery konta demo są zaimportowane do realmu `document-workflow` w Keycloaku. Hasło jest wspólne
dla wszystkich kont demo i pochodzi ze zmiennej środowiskowej `KEYCLOAK_DEMO_USER_PASSWORD`
(wartość ustalana per wdrożenie — zapytaj administratora środowiska).

| Login | Rola | Osoba |
| --- | --- | --- |
| `demo.administrator` | administrator | Adam Nowakowski |
| `demo.referent` | referent | Katarzyna Zielińska |
| `demo.kierownik` | kierownik | Piotr Lewandowski |
| `demo.ksiegowosc` | ksiegowosc | Marta Wiśniewska |

Konsola administracyjna Keycloaka (zarządzanie realnymi kontami logowania — hasła,
włączenie/wyłączenie konta, role realmu) jest osobnym systemem, niezależnym od ekranu "Użytkownicy
i role" w module Administracja — zobacz stronę [Administracja](administracja.md).
