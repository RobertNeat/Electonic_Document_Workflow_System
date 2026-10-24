import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { DokumentObywatelaListElement } from '@electronic-document-workflow/document-shared-dto';
import { environment } from '../../../environments/environment';

/**
 * Minimalny klient HTTP dla GET /api/dokumenty - jedna metoda dla jednego kształtu zapytania
 * (filtr obywatelId, użyty przez zakładkę "Dokumenty" karty obywatela). Pozostałe filtry
 * endpointu (sprawaId/typDokumentuId) i pozostałe operacje modułu Dokumenty (szczegóły, wersje,
 * podpisywanie, szablony) są zadaniem przyszłego cyklu podłączającego główny moduł Dokumenty -
 * nie są tu dodawane na zapas.
 */
@Injectable({ providedIn: 'root' })
export class DokumentyApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.documentApiBaseUrl;

  listaDlaObywatela(obywatelId: string): Observable<DokumentObywatelaListElement[]> {
    const params = new HttpParams().set('obywatelId', obywatelId);
    return this.http.get<DokumentObywatelaListElement[]>(`${this.baseUrl}/api/dokumenty`, {
      params,
    });
  }
}
