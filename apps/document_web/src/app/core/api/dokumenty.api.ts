import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  Certyfikat,
  Dokument,
  DokumentListElement,
  SzablonJasper,
  WersjaDokumentu,
} from '@electronic-document-workflow/document-shared-dto';
import { environment } from '../../../environments/environment';

export interface DokumentyFiltry {
  sprawaId?: string | null;
  typDokumentuId?: string | null;
  obywatelId?: string | null;
}

/**
 * Klient HTTP dla endpointów modułu Dokumenty w `document_api` - lista/szczegóły dokumentu,
 * wersje, szablony Jasper oraz podpisywanie dokumentu (PAdES). `listaCertyfikatow()` woła proxy
 * `GET /api/certyfikaty` (document_api → sign_service) do zasilenia cert-pickera - zwraca
 * WSZYSTKIE certyfikaty, bez filtrowania po tożsamości użytkownika (zob. plan.md). Upload
 * szablonu nie jest tu dodawany na zapas - backend go jeszcze nie wystawia.
 */
@Injectable({ providedIn: 'root' })
export class DokumentyApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.documentApiBaseUrl;

  lista(filtry: DokumentyFiltry = {}): Observable<DokumentListElement[]> {
    let params = new HttpParams();
    if (filtry.sprawaId) params = params.set('sprawaId', filtry.sprawaId);
    if (filtry.typDokumentuId) params = params.set('typDokumentuId', filtry.typDokumentuId);
    if (filtry.obywatelId) params = params.set('obywatelId', filtry.obywatelId);
    return this.http.get<DokumentListElement[]>(`${this.baseUrl}/api/dokumenty`, { params });
  }

  getById(id: string): Observable<Dokument> {
    return this.http.get<Dokument>(`${this.baseUrl}/api/dokumenty/${id}`);
  }

  wersje(dokumentId: string): Observable<WersjaDokumentu[]> {
    return this.http.get<WersjaDokumentu[]>(`${this.baseUrl}/api/dokumenty/${dokumentId}/wersje`);
  }

  szablony(typDokumentuId?: string | null): Observable<SzablonJasper[]> {
    let params = new HttpParams();
    if (typDokumentuId) params = params.set('typDokumentuId', typDokumentuId);
    return this.http.get<SzablonJasper[]>(`${this.baseUrl}/api/szablony`, { params });
  }

  historiaSzablonu(id: string): Observable<SzablonJasper[]> {
    return this.http.get<SzablonJasper[]>(`${this.baseUrl}/api/szablony/${id}/historia`);
  }

  listaCertyfikatow(): Observable<Certyfikat[]> {
    return this.http.get<Certyfikat[]>(`${this.baseUrl}/api/certyfikaty`);
  }

  podpisz(dokumentId: string, certyfikatId: string): Observable<WersjaDokumentu> {
    return this.http.post<WersjaDokumentu>(`${this.baseUrl}/api/dokumenty/${dokumentId}/podpisz`, {
      certyfikatId,
    });
  }
}
