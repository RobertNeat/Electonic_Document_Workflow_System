import { Injectable, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import type {
  DefinicjaObiegu,
  Sprawa,
  UtworzSpraweRequest,
} from '@electronic-document-workflow/document-shared-dto';
import { SprawyApi, SzukajSprawFiltry } from '../../core/api/sprawy.api';

/** Zwraca czytelny komunikat dla błędu HTTP z document_api - wzorzec do powielenia przez kolejne
 * moduły (brak jednego uniwersalnego 403/404/409 dla każdego endpointu, treść zależy od domeny). */
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

/**
 * Przechowuje stan LISTY spraw (filtry/paginacja) współdzielony między listą i formularzem
 * rejestracji - woła realne API `document_api` (SprawyApi), bez lokalnej persystencji. Stan
 * widoku szczegółów jednej sprawy ŻYJE W KOMPONENCIE (SprawaSzczegoly), nie tutaj - jako singleton
 * `providedIn: 'root'` ten store współdzieliłby jedną sprawę/metrykę między wszystkimi otwartymi
 * widokami szczegółów, co przy nawigacji między sprawami pokazywałoby przez chwilę dane poprzedniej.
 */
@Injectable({ providedIn: 'root' })
export class SprawyStore {
  private readonly api = inject(SprawyApi);

  private readonly stanSprawy = signal<Sprawa[]>([]);
  private readonly stanStrona = signal(0);
  private readonly stanRozmiarStrony = signal(20);
  private readonly stanLiczbaWszystkich = signal(0);
  private readonly stanLoading = signal(false);
  private readonly stanBlad = signal<string | null>(null);

  readonly sprawy = this.stanSprawy.asReadonly();
  readonly strona = this.stanStrona.asReadonly();
  readonly rozmiarStrony = this.stanRozmiarStrony.asReadonly();
  readonly liczbaWszystkich = this.stanLiczbaWszystkich.asReadonly();
  readonly loading = this.stanLoading.asReadonly();
  readonly blad = this.stanBlad.asReadonly();

  /** Odświeża listę spraw z danymi filtrami/paginacją (GET /api/sprawy). */
  async szukaj(filtry: SzukajSprawFiltry = {}): Promise<void> {
    this.stanLoading.set(true);
    this.stanBlad.set(null);
    try {
      const strona = await firstValueFrom(this.api.szukaj(filtry));
      this.stanSprawy.set(strona.dane);
      this.stanStrona.set(strona.strona);
      this.stanRozmiarStrony.set(strona.rozmiarStrony);
      this.stanLiczbaWszystkich.set(strona.liczbaWszystkich);
    } catch (err) {
      this.stanBlad.set(komunikatBleduHttp(err, 'Nie udało się wczytać listy spraw.'));
    } finally {
      this.stanLoading.set(false);
    }
  }

  /** Rejestruje nową sprawę (POST /api/sprawy) - `znakSprawy` jest nadawany przez backend, nie
   * generowany lokalnie. Zwraca utworzoną sprawę przy sukcesie, komunikat błędu (string) przy
   * niepowodzeniu - bez pisania do sygnału `blad` tego store'a, żeby komunikat formularza
   * rejestracji nie "przeciekł" później na listę spraw. */
  async dodajSprawe(request: UtworzSpraweRequest): Promise<Sprawa | string> {
    try {
      return await firstValueFrom(this.api.rejestrujSprawe(request));
    } catch (err) {
      return komunikatBleduHttp(err, 'Nie udało się zarejestrować sprawy.');
    }
  }

  async definicjeObiegow(): Promise<DefinicjaObiegu[]> {
    try {
      return await firstValueFrom(this.api.definicjeObiegow());
    } catch {
      return [];
    }
  }
}

/** Formatuje datę jako YYYY-MM-DD w czasie lokalnym (bez przesunięcia przez UTC). */
export function formatujDateLokalna(data: Date): string {
  const rok = data.getFullYear();
  const miesiac = String(data.getMonth() + 1).padStart(2, '0');
  const dzien = String(data.getDate()).padStart(2, '0');
  return `${rok}-${miesiac}-${dzien}`;
}
