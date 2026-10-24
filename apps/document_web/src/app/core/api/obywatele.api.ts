import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { Obywatel } from '@electronic-document-workflow/document-shared-dto';
import { environment } from '../../../environments/environment';

/**
 * Typowany klient HTTP dla endpointów modułu Obywatele w `document_api` - wzorzec powielony
 * z `SprawyApi`. Zwraca Observable, bez logiki stanu/cache (ta żyje w konsumentach).
 */
@Injectable({ providedIn: 'root' })
export class ObywateleApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.documentApiBaseUrl;

  szukaj(query?: string): Observable<Obywatel[]> {
    let params = new HttpParams();
    if (query) params = params.set('query', query);
    return this.http.get<Obywatel[]>(`${this.baseUrl}/api/obywatele`, { params });
  }

  getById(id: string): Observable<Obywatel> {
    return this.http.get<Obywatel>(`${this.baseUrl}/api/obywatele/${id}`);
  }
}
