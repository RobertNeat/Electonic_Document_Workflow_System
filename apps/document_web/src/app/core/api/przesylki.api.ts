import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  KanalWysylki,
  Przesylka,
  StatusPrzesylki,
  UtworzPrzesylkeRequest,
} from '@electronic-document-workflow/document-shared-dto';
import { environment } from '../../../environments/environment';

export interface SzukajPrzesylekFiltry {
  obywatelId?: string | null;
  status?: StatusPrzesylki | null;
  kanal?: KanalWysylki | null;
  kierunek?: Przesylka['kierunek'] | null;
}

/**
 * Typowany klient HTTP dla endpointów modułu Korespondencja w `document_api` - wzorzec
 * powielony z `SprawyApi`. `szukaj` obsługuje pełen zestaw filtrów `GET /api/przesylki`
 * (status/kanal/kierunek/obywatelId) - obywatelId został dodany jako pierwszy w cyklu
 * podłączającym zakładkę "Korespondencja" karty obywatela, pozostałe filtry dołączone przy
 * podłączaniu głównego rejestru/formularza nadania.
 */
@Injectable({ providedIn: 'root' })
export class PrzesylkiApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.documentApiBaseUrl;

  szukaj(filtry: SzukajPrzesylekFiltry): Observable<Przesylka[]> {
    let params = new HttpParams();
    if (filtry.obywatelId) params = params.set('obywatelId', filtry.obywatelId);
    if (filtry.status) params = params.set('status', filtry.status);
    if (filtry.kanal) params = params.set('kanal', filtry.kanal);
    if (filtry.kierunek) params = params.set('kierunek', filtry.kierunek);
    return this.http.get<Przesylka[]>(`${this.baseUrl}/api/przesylki`, { params });
  }

  getById(id: string): Observable<Przesylka> {
    return this.http.get<Przesylka>(`${this.baseUrl}/api/przesylki/${id}`);
  }

  nadaj(request: UtworzPrzesylkeRequest): Observable<Przesylka> {
    return this.http.post<Przesylka>(`${this.baseUrl}/api/przesylki`, request);
  }
}
