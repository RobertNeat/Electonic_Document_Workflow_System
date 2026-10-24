import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { Przesylka } from '@electronic-document-workflow/document-shared-dto';
import { environment } from '../../../environments/environment';

export interface SzukajPrzesylekFiltry {
  obywatelId?: string | null;
}

/**
 * Minimalny klient HTTP dla GET /api/przesylki - jedna metoda dla jednego kształtu zapytania
 * (filtr obywatelId, użyty przez zakładkę "Korespondencja" karty obywatela). Pozostałe filtry
 * endpointu (status/kanal/kierunek) i pozostałe operacje modułu Korespondencja (nadanie pisma,
 * szczegóły) są zadaniem przyszłego cyklu podłączającego główny moduł Korespondencja - nie są
 * tu dodawane na zapas.
 */
@Injectable({ providedIn: 'root' })
export class PrzesylkiApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.documentApiBaseUrl;

  szukaj(filtry: SzukajPrzesylekFiltry): Observable<Przesylka[]> {
    let params = new HttpParams();
    if (filtry.obywatelId) params = params.set('obywatelId', filtry.obywatelId);
    return this.http.get<Przesylka[]>(`${this.baseUrl}/api/przesylki`, { params });
  }
}
