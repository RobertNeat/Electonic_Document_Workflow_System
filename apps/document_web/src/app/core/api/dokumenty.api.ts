import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  Certyfikat,
  Dokument,
  DokumentListElement,
  SzablonJasper,
  SzczegolyPodpisu,
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
 * wersje, pobranie treści pliku PDF danej wersji (proxy do dms_service), szablony Jasper oraz
 * podpisywanie dokumentu (PAdES). `listaCertyfikatow()` woła proxy `GET /api/certyfikaty`
 * (document_api → sign_service) do zasilenia cert-pickera - zwraca WSZYSTKIE certyfikaty, bez
 * filtrowania po tożsamości użytkownika (zob. plan.md).
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

  // Treść pliku PDF danej wersji - `responseType: 'blob'` (binarne dane, nie JSON). Używane przez
  // front do podglądu (iframe na object URL) i pobrania (tymczasowy <a download>).
  pobierzPlik(dokumentId: string, wersja: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/api/dokumenty/${dokumentId}/wersje/${wersja}/plik`, {
      responseType: 'blob',
    });
  }

  szablony(typDokumentuId?: string | null): Observable<SzablonJasper[]> {
    let params = new HttpParams();
    if (typDokumentuId) params = params.set('typDokumentuId', typDokumentuId);
    return this.http.get<SzablonJasper[]>(`${this.baseUrl}/api/szablony`, { params });
  }

  historiaSzablonu(id: string): Observable<SzablonJasper[]> {
    return this.http.get<SzablonJasper[]>(`${this.baseUrl}/api/szablony/${id}/historia`);
  }

  // Content-Type multipart/form-data jest ustawiany automatycznie przez HttpClient na podstawie
  // FormData - nie ustawiać nagłówka ręcznie (brak boundary spowodowałby błąd parsowania).
  wgrajSzablon(typDokumentuId: string, nazwa: string, plik: File): Observable<SzablonJasper> {
    const formData = new FormData();
    formData.append('typDokumentuId', typDokumentuId);
    formData.append('nazwa', nazwa);
    formData.append('plik', plik, plik.name);
    return this.http.post<SzablonJasper>(`${this.baseUrl}/api/szablony`, formData);
  }

  listaCertyfikatow(): Observable<Certyfikat[]> {
    return this.http.get<Certyfikat[]>(`${this.baseUrl}/api/certyfikaty`);
  }

  podpisz(dokumentId: string, certyfikatId: string): Observable<WersjaDokumentu> {
    return this.http.post<WersjaDokumentu>(`${this.baseUrl}/api/dokumenty/${dokumentId}/podpisz`, {
      certyfikatId,
    });
  }

  // Realna weryfikacja podpisu w sign_service (nie lokalne dane) - zob. kontrakt DTO co do null
  // dla czasPodpisu/znacznikCzasuTsa.
  szczegolyPodpisu(dokumentId: string, wersja: number): Observable<SzczegolyPodpisu> {
    return this.http.get<SzczegolyPodpisu>(
      `${this.baseUrl}/api/dokumenty/${dokumentId}/wersje/${wersja}/podpis`,
    );
  }
}
