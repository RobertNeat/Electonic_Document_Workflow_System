import { HttpErrorResponse } from '@angular/common/http';

/** Zwraca czytelny komunikat dla błędu HTTP z document_api - wzorzec współdzielony między
 * modułami (Sprawy, Obywatele, ...), bo treść 403/404/sieć jest identyczna niezależnie od
 * domeny; tylko komunikat domyślny (409/walidacja specyficzna dla endpointu) różni się per
 * wywołanie. */
export function komunikatBleduHttp(err: unknown, domyslny: string): string {
  if (!(err instanceof HttpErrorResponse)) {
    return domyslny;
  }
  if (err.status === 0) {
    return 'Brak połączenia z serwerem (sieć lub CORS).';
  }
  if (err.status === 403) {
    return 'Brak uprawnień do wykonania tej operacji.';
  }
  if (err.status === 404) {
    return 'Nie znaleziono żądanego zasobu.';
  }
  const komunikatZBackendu = (err.error as { komunikat?: string } | null)?.komunikat;
  return komunikatZBackendu ?? domyslny;
}
