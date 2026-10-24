import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  Dokument,
  DokumentListElement,
  DokumentObywatelaListElement,
} from '@electronic-document-workflow/document-shared-dto';
import { environment } from '../../../environments/environment';

/**
 * Klient HTTP dla endpointów GET modułu Dokumenty w `document_api`, potrzebnych przez inne
 * moduły (zakładka "Dokumenty" karty obywatela, formularz nadania pisma i szczegóły przesyłki
 * w module Korespondencja). Operacje mutujące (podpisywanie, szablony) są zadaniem przyszłego
 * cyklu podłączającego własny moduł Dokumenty - nie są tu dodawane na zapas.
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

  listaDlaSprawy(sprawaId: string): Observable<DokumentListElement[]> {
    const params = new HttpParams().set('sprawaId', sprawaId);
    return this.http.get<DokumentListElement[]>(`${this.baseUrl}/api/dokumenty`, { params });
  }

  getById(id: string): Observable<Dokument> {
    return this.http.get<Dokument>(`${this.baseUrl}/api/dokumenty/${id}`);
  }
}
