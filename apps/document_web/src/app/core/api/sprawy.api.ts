import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  DefinicjaObiegu,
  PozycjaJrwa,
  Sprawa,
  StronaWynikow,
  UtworzSpraweRequest,
  WpisMetryki,
  ZmienStanSprawyRequest,
} from '@electronic-document-workflow/document-shared-dto';
import { environment } from '../../../environments/environment';

export interface SzukajSprawFiltry {
  stan?: string | null;
  wydzial?: string | null;
  q?: string | null;
  obywatelId?: string | null;
  strona?: number;
  rozmiarStrony?: number;
}

/**
 * Typowany klient HTTP dla endpointów modułu Sprawy w `document_api` - wzorzec do powielenia
 * przez kolejne moduły (Obywatele, Dokumenty, ...) w następnych cyklach. Zwraca Observable
 * (konwersja do sygnałów odbywa się w `SprawyStore`, nie tutaj), bez logiki stanu/cache.
 */
@Injectable({ providedIn: 'root' })
export class SprawyApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.documentApiBaseUrl;

  szukaj(filtry: SzukajSprawFiltry): Observable<StronaWynikow<Sprawa>> {
    let params = new HttpParams();
    if (filtry.stan) params = params.set('stan', filtry.stan);
    if (filtry.wydzial) params = params.set('wydzial', filtry.wydzial);
    if (filtry.q) params = params.set('q', filtry.q);
    if (filtry.obywatelId) params = params.set('obywatelId', filtry.obywatelId);
    if (filtry.strona !== undefined) params = params.set('strona', filtry.strona);
    if (filtry.rozmiarStrony !== undefined)
      params = params.set('rozmiarStrony', filtry.rozmiarStrony);
    return this.http.get<StronaWynikow<Sprawa>>(`${this.baseUrl}/api/sprawy`, { params });
  }

  getById(id: string): Observable<Sprawa> {
    return this.http.get<Sprawa>(`${this.baseUrl}/api/sprawy/${id}`);
  }

  rejestrujSprawe(request: UtworzSpraweRequest): Observable<Sprawa> {
    return this.http.post<Sprawa>(`${this.baseUrl}/api/sprawy`, request);
  }

  zmienStan(id: string, nowyStan: string): Observable<Sprawa> {
    const body: ZmienStanSprawyRequest = { nowyStan };
    return this.http.patch<Sprawa>(`${this.baseUrl}/api/sprawy/${id}/stan`, body);
  }

  metryka(id: string): Observable<WpisMetryki[]> {
    return this.http.get<WpisMetryki[]>(`${this.baseUrl}/api/sprawy/${id}/metryka`);
  }

  definicjeObiegow(): Observable<DefinicjaObiegu[]> {
    return this.http.get<DefinicjaObiegu[]>(`${this.baseUrl}/api/definicje-obiegow`);
  }

  jrwa(): Observable<PozycjaJrwa[]> {
    return this.http.get<PozycjaJrwa[]>(`${this.baseUrl}/api/slowniki/jrwa`);
  }
}
