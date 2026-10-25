## Do zrobienia (bieżąca sesja)

Brak.

## Do zrobienia (kolejne sesje) — wg priorytetu

1. **Moduł Wymiana EDI/XML — podłączenie do rzeczywistego API.** Backend już wystawia
   `POST /api/wymiana/xml`, `POST /api/wymiana/ubl`, `GET /api/wymiana/xml/historia`,
   `GET /api/wymiana/ubl/historia` oraz `POST /api/akcje-zbiorowe` (jeden endpoint z
   dyskryminatorem `typAkcji`) + `GET /api/akcje-zbiorowe/:akcjaId/status` — frontend
   (`features/wymiana/`) wciąż w 100% symuluje działanie w przeglądarce
   (`setTimeout`/`setInterval`), zero wywołań HTTP. Zob.
   `currentwork/contracts/document_api_wymiana.md`.
2. **Moduł Pulpit — brak dedykowanego backendu, do zbudowania z istniejących zasobów.**
   Nadal korzysta z `core/mock/mock-data.ts` i `pulpit-mock.ts` (w tym sprawy z ID
   nieistniejącymi w głównym mocku, więc routerLink do szczegółów sprawy nie działa
   nawet w trybie mock). Docelowo oparty na istniejących `GET /api/sprawy`,
   `GET /api/naleznosci`, `GET /api/przesylki` z odpowiednimi filtrami (`referent`,
   sortowanie po `terminUstawowy`, `wydzial`, `status`) — zob.
   `currentwork/contracts/document_api_pulpit.md` co do dokładnych zapytań.
3. **Drobne pozostałości mockowe w module Sprawy.** `sprawa-szczegoly.ts` korzysta z
    `MOCK_TYPY_DOKUMENTOW` (brak podłączonego słownika typów dokumentów), `sprawa-nowa.ts`
    z `MOCK_OBYWATELE` przy wyborze obywatela/podmiotu dla nowej sprawy — drobne, ale
    realne niedoróbki w module uznanym za "podłączony".
4. **Brak parametru sortowania w `GET /api/sprawy`.** Lista jest tylko paginowana
    (`strona`/`rozmiarStrony`) — do rozważenia `sort=pole,asc|desc` przy przyszłym
    rozszerzeniu, drugorzędne.
5. **Testy jednostkowe `jasper_service`.** Najmniej przetestowany moduł backendowy —
    tylko test ładowania kontekstu i kontrolera. Brak testu `RenderowanieService`
    (kompilacja/fill/eksport PDF, obsługa uszkodzonego `.jrxml`) i jego `DmsServiceClient`.
