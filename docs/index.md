---
title: Strona główna
nav_order: 1
description: Dokumentacja funkcjonalna systemu EDWS
permalink: /
---

# Electronic Document Workflow System — dokumentacja

System klasy enterprise do elektronicznego obiegu dokumentów i rozliczania płatności, wzorowany na
systemach EZD (Elektroniczne Zarządzanie Dokumentacją) stosowanych w jednostkach administracji
publicznej w Polsce.

Architektura to zestaw współpracujących mikroserwisów: główne API spraw i dokumentów
(`document_api`), dedykowany system zarządzania dokumentami (`dms_service`), silnik renderujący
szablony JasperReports (`jasper_service`), serwis podpisu elektronicznego z własną demonstracyjną
infrastrukturą PKI (`sign_service`), symulator dostawcy doręczeń (`delivery_mock`), Keycloak jako
dostawca tożsamości (OIDC/OAuth2) i RabbitMQ jako broker komunikatów dla akcji asynchronicznych.

To wiki opisuje funkcjonalność systemu z perspektywy użytkownika — każda strona odpowiada jednemu
modułowi widocznemu w menu aplikacji `document_web`, z osadzonymi zrzutami ekranu i animacjami (gif)
pokazującymi rzeczywisty przebieg pracy.

## Spis modułów

- [Architektura i standardy](architektura.md)
- [Logowanie, role i konta demo](logowanie-i-role.md)
- [Pulpit użytkownika](pulpit.md)
- [Sprawy i obiegi](sprawy-i-obiegi.md)
- [Dokumenty, szablony i podpis elektroniczny](dokumenty-i-podpis.md)
- [Korespondencja i wysyłka pism](korespondencja.md)
- [Rozliczanie płatności](platnosci.md)
- [Karta obywatela/podmiotu](karta-obywatela.md)
- [Wymiana EDI/XML i akcje zbiorowe](wymiana-edi-xml.md)
- [Administracja](administracja.md)

## Jak korzystać z tego wiki

Każda strona modułu ma ten sam układ: krótki opis funkcji, role biorące udział, przebieg pracy z
ekranami (zrzuty i gify), szczegóły implementacji (jak moduł jest zbudowany pod maską) oraz znane
ograniczenia — czego nie da się zrobić w aktualnej wersji aplikacji.
