# Kontrakt: document_api ↔ frontend (moduł Korespondencja)

Moduł Korespondencja jest podłączony pod rzeczywiste `document_api` (`GET /api/przesylki`,
`GET /api/przesylki/:id`, `POST /api/przesylki`). Jedna pozycja pozostaje nieadresowana:

- Formularz nadania pisma (`korespondencja-wyslij`) pokazuje tylko ostrzeżenie UI, gdy wybrany
  adresat nie ma wypełnionego `adresDoreczenEDE`, ale nie blokuje wyboru kanału e-Doręczenia.
  `KorespondencjaService.nadajPismo` (backend) NIE weryfikuje tego pola przed utworzeniem
  przesyłki kanałem `e-doreczenia` — przyjmie żądanie nawet gdy obywatel nie ma zarejestrowanej
  skrzynki ADE. To jest prawdziwa, wciąż otwarta luka backendu (nie workaround frontendu) do
  rozważenia w przyszłym cyklu dot. modułu Korespondencja/walidacji ADE.
