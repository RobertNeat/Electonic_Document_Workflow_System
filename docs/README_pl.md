<p align="center">
  <img src="../apps/document_web/public/document.png" width="150" alt="Logo EDWS">
</p>

<h1 align="center">Electronic Document Workflow System</h1>

<p align="center">
  <a href="https://robertneat.github.io/Electonic_Document_Workflow_System/">
    <img src="https://img.shields.io/badge/Documentation-4d8c42?style=for-the-badge" alt="Documentation">
  </a>
  <br>
  <a href="https://github.com/RobertNeat/Electonic_Document_Workflow_System/blob/main/README.md">
    <img src="https://img.shields.io/badge/English_version-4d8c42?style=for-the-badge" alt="English version">
  </a>
</p>

System klasy enterprise do elektronicznego obiegu dokumentów i spraw, wzorowany na systemach EZD
(Elektroniczne Zarządzanie Dokumentacją) stosowanych w jednostkach administracji publicznej.
Zbudowany jako architektura mikroserwisowa — centralne API spraw/dokumentów, dedykowany system DMS,
silnik renderujący JasperReports, serwis podpisu elektronicznego z własną demonstracyjną
infrastrukturą PKI, symulator doręczeń, Keycloak i RabbitMQ — odzwierciedlając sposób, w jaki realny
back office administracji publicznej jest rozdzielony między wyspecjalizowane systemy, a nie zebrany
w jednym monolicie.

## Wykorzystane standardy obiegu dokumentów

- **JRWA** — Jednolity Rzeczowy Wykaz Akt (jednolita klasyfikacja akt) determinuje znak sprawy i
  kategorię archiwalną (A/B).
- **DMS (zarządzanie dokumentami)** — wersjonowane przechowywanie szablonów dokumentów i
  wygenerowanych dokumentów, z wyraźnym rozdzieleniem wersji roboczej (niepodpisanej) i finalnej
  (podpisanej) dokumentu.
- **XAdES / PAdES (ETSI)** — podpis elektroniczny dokumentów PDF (PAdES) i XML (XAdES) z użyciem
  biblioteki EU DSS, na demonstracyjnej infrastrukturze PKI (hierarchia CA, responder OCSP/CRL);
  XAdES jest zaimplementowany w serwisie podpisu, ale nie jest jeszcze podłączony end-to-end.
- **OIDC / OAuth2** — Keycloak jako dostawca tożsamości; `document_api` waliduje tokeny JWT jako
  serwer zasobów OAuth2 i mapuje role realmu na uprawnienia Spring Security (RBAC).
- **Architektura EZD** — rejestr spraw, metryka sprawy (historia czynności), rejestr korespondencji
  wpływającej/wychodzącej oraz dziennik zdarzeń zgodne z konwencjami systemów klasy EZD.
- **CAMT.053 (ISO 20022)** — import wyciągów bankowych z realnym parsowaniem struktury standardu i
  automatycznym dopasowaniem wpłat do należności.
- **EDI/XML** — import komunikatów sprawy (XML) oraz dokumentów/faktur kosztowych (UBL/PEPPOL) od
  podmiotów zewnętrznych, z walidacją strukturalną; obecnie wyłącznie import, bez podłączenia do
  realnej sieci PEPPOL.

## Co robi EDWS

- **Obieg spraw i dokumentów** — rejestracja spraw w ramach konfigurowalnych definicji obiegu (stany,
  przejścia, wymagane role/dokumenty dla danego rodzaju sprawy), prowadzenie historii sprawy oraz
  wymuszanie kontroli ról i dokumentów przy zmianie stanu.

![Obieg sprawy](../docs/media/1.%20przeprowadzenie%20sprawy/przeprowadzenie_sprawy.gif)

- **Zarządzanie dokumentami i szablonami (DMS)** — wgrywanie i wersjonowanie szablonów JasperReports,
  generowanie dokumentów z szablonu i danych sprawy, przeglądanie historii wersji dokumentu z
  rozróżnieniem wersji roboczej (niepodpisanej) i finalnej (podpisanej).

![Wgranie szablonu przez administratora](../docs/media/2.%20dokumenty%20i%20podpis/1.%20wgranie%20szablonu%20jasper%20przez%20administratora.png)

- **Podpis elektroniczny** — podpisywanie wygenerowanych dokumentów PDF certyfikatem użytkownika,
  weryfikacja statusu podpisu (poprawny/niepoprawny/wygasły/unieważniony) przez OCSP/CRL oraz podgląd
  szczegółów podpisu (podpisujący, czas, odcisk certyfikatu).

![Podpis elektroniczny](../docs/media/2.%20dokumenty%20i%20podpis/złożenie_podpisu.gif)

- **Korespondencja i wysyłka** — nadawanie podpisanych pism do obywateli/podmiotów przez e-Doręczenia,
  operatora pocztowego (przesyłka hybrydowa) lub e-mail, ze śledzeniem statusu doręczenia
  (nadane/doręczone/odebrane/zwrot/awizo) przez asynchroniczny kanał statusów.

![Korespondencja](../docs/media/3.%20wysyłka%20korespondencji/wysyłka_korespondencji.gif)

- **Rozliczanie płatności** — rejestr należności powiązanych ze sprawą, import wyciągów bankowych z
  automatycznym dopasowaniem wpłat, ręczna rekoncyliacja niedopasowanych wpłat oraz generowanie
  przypomnień o należnościach przeterminowanych.

![Płatności](../docs/media/4.%20płatności/1.%20przegląd%20rejestru%20należności.png)

- **Karta obywatela/podmiotu** — wyszukiwanie obywateli i podmiotów oraz przegląd zunifikowanego
  profilu obejmującego dane podstawowe, sprawy, dokumenty, korespondencję i rozrachunki w jednym
  miejscu.

![Karta obywatela](../docs/media/5.%20karta%20obywatelapodmiotu/karta_obywatela.gif)

- **Wymiana EDI/XML** — import komunikatów sprawy (XML) oraz dokumentów/faktur kosztowych
  (UBL/PEPPOL) od podmiotów zewnętrznych, z walidacją strukturalną i mapowaniem do modelu domenowego.
- **Akcje zbiorowe** — masowa zmiana stanu spraw, masowa wysyłka pism lub masowe generowanie
  dokumentów na wielu sprawach naraz, przetwarzane asynchronicznie z paskiem postępu.
- **Pulpit użytkownika** — strona startowa podsumowująca sprawy w toku, zbliżające się/przekroczone
  terminy ustawowe, nierozliczone należności i oczekujące doręczenia dla zalogowanego użytkownika.
- **Administracja** — zarządzanie profilami i rolami użytkowników, słownikami referencyjnymi (JRWA,
  typy dokumentów, dostawcy wysyłki, stawki opłat) oraz przegląd dziennika zdarzeń dla operacji na
  sprawach, dokumentach i płatnościach.

## Pełna dokumentacja

Szczegółowa dokumentacja funkcjonalna każdego modułu (role, przebieg pracy z ekranami, szczegóły
implementacji, znane ograniczenia) jest dostępna w wiki:
**[robertneat.github.io/Electonic_Document_Workflow_System](https://robertneat.github.io/Electonic_Document_Workflow_System/)**.
