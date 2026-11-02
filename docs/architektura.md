---
title: Architektura i standardy
nav_order: 2
---

# Architektura i standardy

## Mikroserwisy

System jest podzielony na wyspecjalizowane serwisy, odzwierciedlając sposób, w jaki realny back
office administracji publicznej jest rozdzielony między systemy, a nie zebrany w jednym monolicie.

| Serwis | Rola | Port |
| --- | --- | --- |
| `document_api` | główny moduł domenowy — sprawy, obiegi, strony, należności/wpłaty, orkiestracja wywołań do pozostałych serwisów | 10620 |
| `document_web` | frontend (Angular + PrimeNG) dla użytkowników końcowych | 10600 |
| `dms_service` | przechowywanie i wersjonowanie szablonów Jasper oraz wygenerowanych dokumentów | 10621 |
| `jasper_service` | bezstanowy silnik renderujący dokumenty z szablonu `.jrxml` do PDF | 10622 |
| `sign_service` | podpis elektroniczny (PAdES/XAdES), demonstracyjna infrastruktura PKI, OCSP/CRL | 10623 |
| `delivery_mock` | symulator dostawcy wysyłki (e-Doręczenia, operator pocztowy) z własnym panelem | 10624 |
| `keycloak` | centralne uwierzytelnianie i autoryzacja (OIDC/OAuth2) | 10681 |
| `rabbitmq` | broker komunikatów dla akcji zbiorowych i zdarzeń asynchronicznych | 10670/10671 |

Jedna baza danych PostgreSQL, współdzielona przez wszystkie serwisy — separacja wyłącznie na
poziomie schem (`core`, `dms`, `delivery`, `pki`, `keycloak`), każdy serwis łączy się własną rolą
bazodanową ograniczoną do swojej schemy. Pliki binarne nie są przechowywane w bazie — każdy serwis
przechowujący pliki ma własny, dedykowany wolumen Docker.

## Standardy dokumentów elektronicznych

| Standard | Zakres | Sposób realizacji |
| --- | --- | --- |
| **JRWA** | Jednolity Rzeczowy Wykaz Akt — klasyfikacja spraw | W pełni — słownik JRWA jako dane referencyjne, znak sprawy budowany na jego podstawie |
| **Kategorie archiwalne (A/B)** | Kwalifikacja archiwalna dokumentacji | W pełni — pole kategorii archiwalnej uzupełniane automatycznie z pozycji JRWA |
| **EZD** | Zasady obiegu spraw/dokumentów elektronicznych w administracji | W pełni funkcjonalnie jako architektura `document_api` + DMS, bez certyfikacji zgodności z konkretnym systemem klasy EZD RP |
| **e-Doręczenia (ADE/BAE)** | Doręczenia elektroniczne o skutku prawnym doręczenia | Zmockowane w `delivery_mock` — adres ADE, dowody wysłania/otrzymania na wzór ADE/BAE, bez integracji z realną bazą adresów elektronicznych |
| **XAdES / PAdES (ETSI)** | Podpis elektroniczny dla XML/PDF | W pełni (biblioteka EU DSS) na demonstracyjnej infrastrukturze PKI — podpis funkcjonalnie poprawny, certyfikaty nie są wydane przez zaufane CA; w aplikacji używany jest tylko PAdES (PDF), XAdES nie jest jeszcze wywoływany end-to-end |
| **RFC 3161 (TSA)** | Znakowanie czasem | Zmockowane — własny serwer TSA zgodny ze standardem, niepubliczny/niekwalifikowany |
| **UBL 2.1 / PEPPOL (EDI)** | Elektroniczna wymiana faktur/dokumentów kosztowych | Zmockowane — obsługa formatu UBL, bez podłączenia do realnej sieci PEPPOL |
| **CAMT.053 (ISO 20022)** | Elektroniczny wyciąg bankowy | W pełni — parsowanie i import rzeczywistego formatu CAMT.053 |
| **OIDC / OAuth2** | Uwierzytelnianie i autoryzacja | W pełni — Keycloak jako dostawca identity |

## Integracje między serwisami

- **`document_api` ↔ `dms_service`** — REST/JSON, synchroniczne; `document_api` pobiera/zapisuje
  bajty wersji dokumentów i szablonów, `dms_service` nadaje autorytatywne `id`/`wersja`.
- **`document_api` ↔ `jasper_service`** — REST/JSON; renderowanie PDF z szablonu i danych sprawy,
  `jasper_service` jest bezstanowy.
- **`document_api` ↔ `sign_service`** — REST/JSON; złożenie podpisu PAdES i weryfikacja statusu
  (OCSP/CRL) na dokumencie pobranym z `dms_service`.
- **`document_api` ↔ `delivery_mock`** — nadanie przesyłki jest zapisem lokalnym w `document_api`;
  zmiana statusu doręczenia wraca asynchronicznie przez RabbitMQ.
- **`document_api` ↔ RabbitMQ** — akcje zbiorowe (masowa zmiana stanu, wysyłka pism, generowanie
  dokumentów) są publikowane na kolejki i przetwarzane w tle, z odpytywaniem postępu przez frontend.

Zobacz też stronę [Logowanie, role i konta demo](logowanie-i-role.md) dla modelu uwierzytelniania.
