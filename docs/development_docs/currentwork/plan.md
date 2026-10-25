## Do zrobienia (bieżąca sesja)

Brak.

## Do zrobienia (kolejne sesje) — wg priorytetu

1. **Drobne pozostałości mockowe w module Sprawy.** `sprawa-szczegoly.ts` korzysta z
    `MOCK_TYPY_DOKUMENTOW` (brak podłączonego słownika typów dokumentów), `sprawa-nowa.ts`
    z `MOCK_OBYWATELE` przy wyborze obywatela/podmiotu dla nowej sprawy — drobne, ale
    realne niedoróbki w module uznanym za "podłączony".
2. **Brak parametru sortowania w `GET /api/sprawy`.** Lista jest tylko paginowana
    (`strona`/`rozmiarStrony`) — do rozważenia `sort=pole,asc|desc` przy przyszłym
    rozszerzeniu, drugorzędne.
3. **Testy jednostkowe `jasper_service`.** Najmniej przetestowany moduł backendowy —
    tylko test ładowania kontekstu i kontrolera. Brak testu `RenderowanieService`
    (kompilacja/fill/eksport PDF, obsługa uszkodzonego `.jrxml`) i jego `DmsServiceClient`.
