# Kontrakt: `document_api` ↔ `document_web` — moduł Administracja (gap)

Wszystkie endpointy modułu Administracja (użytkownicy/role, słowniki, dziennik zdarzeń,
definicje obiegów, odczyt struktury organizacyjnej) są zaimplementowane po stronie
`document_api` i podłączone po stronie `document_web`. Jedyny pozostały gap:

## Struktura organizacyjna — zarządzanie (poza zakresem)

- Backend wystawia tylko `GET /api/struktura-organizacyjna` (podgląd, zob.
  `StrukturaOrganizacyjnaController`). Nie ma endpointów `POST`/`PUT` do zarządzania
  wydziałami/stanowiskami (dodawanie, edycja, usuwanie) — front renderuje strukturę
  wyłącznie w trybie odczytu (`features/administracja/struktura`).
