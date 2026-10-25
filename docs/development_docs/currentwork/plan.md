## Do zrobienia (bieżąca sesja)

Brak.

## Do zrobienia (kolejne sesje) — wg priorytetu

1. **Brak parametru sortowania w `GET /api/sprawy`.** Lista jest tylko paginowana
    (`strona`/`rozmiarStrony`) — do rozważenia `sort=pole,asc|desc` przy przyszłym
    rozszerzeniu, drugorzędne.
2. **Testy jednostkowe `jasper_service`.** Najmniej przetestowany moduł backendowy —
    tylko test ładowania kontekstu i kontrolera. Brak testu `RenderowanieService`
    (kompilacja/fill/eksport PDF, obsługa uszkodzonego `.jrxml`) i jego `DmsServiceClient`.
