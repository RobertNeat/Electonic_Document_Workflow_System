import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  Naleznosc,
  NaleznoscListElement,
  StatusRozliczenia,
  Wplata,
  WynikImportuCamt053,
} from '@electronic-document-workflow/document-shared-dto';
import { environment } from '../../../environments/environment';

export interface SzukajNaleznosciFiltry {
  status?: StatusRozliczenia | null;
  obywatelId?: string | null;
}

export interface SzukajWplatFiltry {
  naleznoscId?: string | null;
  obywatelId?: string | null;
}

/**
 * Typowany klient HTTP dla endpointów modułu Płatności w `document_api`. Powstał w cyklu
 * podłączającym zakładkę "Rozrachunki" karty obywatela (wyłącznie filtr obywatelId), rozszerzony
 * w cyklu podłączającym główny rejestr/szczegóły/import modułu Płatności o filtr status
 * (rejestr należności), filtr naleznoscId (wpłaty dla jednej należności), `getById`,
 * `dopasujWplate` i `importujCamt053`. `POST /api/naleznosci/:id/wezwanie` NIE jest tu
 * wystawiane - endpoint nie istnieje po stronie backendu (wymaga jeszcze nieistniejącego
 * jasper_service), zob. kontrakt `currentwork/contracts/document_api_platnosci.md`.
 */
@Injectable({ providedIn: 'root' })
export class PlatnosciApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.documentApiBaseUrl;

  naleznosci(filtry: SzukajNaleznosciFiltry): Observable<NaleznoscListElement[]> {
    let params = new HttpParams();
    if (filtry.status) params = params.set('status', filtry.status);
    if (filtry.obywatelId) params = params.set('obywatelId', filtry.obywatelId);
    return this.http.get<NaleznoscListElement[]>(`${this.baseUrl}/api/naleznosci`, { params });
  }

  getById(id: string): Observable<Naleznosc> {
    return this.http.get<Naleznosc>(`${this.baseUrl}/api/naleznosci/${id}`);
  }

  wplaty(filtry: SzukajWplatFiltry): Observable<Wplata[]> {
    let params = new HttpParams();
    if (filtry.naleznoscId) params = params.set('naleznoscId', filtry.naleznoscId);
    if (filtry.obywatelId) params = params.set('obywatelId', filtry.obywatelId);
    return this.http.get<Wplata[]>(`${this.baseUrl}/api/wplaty`, { params });
  }

  dopasujWplate(wplataId: string, naleznoscId: string): Observable<Wplata> {
    return this.http.post<Wplata>(`${this.baseUrl}/api/wplaty/${wplataId}/dopasuj`, {
      naleznoscId,
    });
  }

  // Pole formularza multipart nazwane "plik" - tak nazwany parametr @RequestParam w
  // PlatnosciController.importujCamt053 (zgodnie z polskim nazewnictwem reszty API).
  importujCamt053(plik: File): Observable<WynikImportuCamt053> {
    const formData = new FormData();
    formData.append('plik', plik);
    return this.http.post<WynikImportuCamt053>(`${this.baseUrl}/api/import-camt053`, formData);
  }
}
