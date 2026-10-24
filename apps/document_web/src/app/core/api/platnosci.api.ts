import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  NaleznoscListElement,
  Wplata,
} from '@electronic-document-workflow/document-shared-dto';
import { environment } from '../../../environments/environment';

export interface ObywatelPlatnosciFiltry {
  obywatelId: string;
}

/**
 * Minimalny klient HTTP dla GET /api/naleznosci i GET /api/wplaty - po jednej metodzie dla
 * jedynego kształtu zapytania potrzebnego zakładce "Rozrachunki" karty obywatela (filtr
 * obywatelId). Pozostałe filtry obu endpointów (status/naleznoscId) i pozostałe operacje
 * modułu Płatności (import CAMT.053, dopasowanie wpłaty) są zadaniem przyszłego cyklu
 * podłączającego główny moduł Płatności - nie są tu dodawane na zapas.
 */
@Injectable({ providedIn: 'root' })
export class PlatnosciApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.documentApiBaseUrl;

  naleznosci(filtry: ObywatelPlatnosciFiltry): Observable<NaleznoscListElement[]> {
    const params = new HttpParams().set('obywatelId', filtry.obywatelId);
    return this.http.get<NaleznoscListElement[]>(`${this.baseUrl}/api/naleznosci`, { params });
  }

  wplaty(filtry: ObywatelPlatnosciFiltry): Observable<Wplata[]> {
    const params = new HttpParams().set('obywatelId', filtry.obywatelId);
    return this.http.get<Wplata[]>(`${this.baseUrl}/api/wplaty`, { params });
  }
}
