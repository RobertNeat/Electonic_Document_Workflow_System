import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  AktualizujRoleUzytkownikaRequest,
  DostawcaWysylki,
  RolaUzytkownika,
  StawkaOplaty,
  UtworzUzytkownikaRequest,
  Uzytkownik,
  Wydzial,
  WpisDziennikaZdarzen,
} from '@electronic-document-workflow/document-shared-dto';
import { environment } from '../../../environments/environment';

export interface AuditLogFiltry {
  typOperacji?: string | null;
  od?: Date | null;
  do?: Date | null;
}

/**
 * Typowany klient HTTP dla endpointów modułu Administracja (poza zasobami, które już mają
 * wrapper w `SprawyApi`: definicje obiegów, JRWA, typy dokumentów).
 */
@Injectable({ providedIn: 'root' })
export class AdministracjaApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.documentApiBaseUrl;

  uzytkownicy(wydzial?: string | null, rola?: RolaUzytkownika | null): Observable<Uzytkownik[]> {
    let params = new HttpParams();
    if (wydzial) params = params.set('wydzial', wydzial);
    if (rola) params = params.set('rola', rola);
    return this.http.get<Uzytkownik[]>(`${this.baseUrl}/api/uzytkownicy`, { params });
  }

  utworzUzytkownika(dane: UtworzUzytkownikaRequest): Observable<Uzytkownik> {
    return this.http.post<Uzytkownik>(`${this.baseUrl}/api/uzytkownicy`, dane);
  }

  aktualizujRoleUzytkownika(id: string, role: RolaUzytkownika[]): Observable<Uzytkownik> {
    const body: AktualizujRoleUzytkownikaRequest = { role };
    return this.http.put<Uzytkownik>(`${this.baseUrl}/api/uzytkownicy/${id}/role`, body);
  }

  strukturaOrganizacyjna(): Observable<Wydzial[]> {
    return this.http.get<Wydzial[]>(`${this.baseUrl}/api/struktura-organizacyjna`);
  }

  dostawcyWysylki(): Observable<DostawcaWysylki[]> {
    return this.http.get<DostawcaWysylki[]>(`${this.baseUrl}/api/slowniki/dostawcy-wysylki`);
  }

  aktualizujDostawceWysylki(id: string, dane: DostawcaWysylki): Observable<DostawcaWysylki> {
    return this.http.put<DostawcaWysylki>(
      `${this.baseUrl}/api/slowniki/dostawcy-wysylki/${id}`,
      dane,
    );
  }

  stawkiOplat(): Observable<StawkaOplaty[]> {
    return this.http.get<StawkaOplaty[]>(`${this.baseUrl}/api/slowniki/stawki-oplat`);
  }

  aktualizujStawkeOplaty(id: string, dane: StawkaOplaty): Observable<StawkaOplaty> {
    return this.http.put<StawkaOplaty>(`${this.baseUrl}/api/slowniki/stawki-oplat/${id}`, dane);
  }

  // `od`/`do` to LocalDate (ISO yyyy-MM-dd) po stronie backendu - zakres inkluzywny po obu
  // stronach, konwersję na granicę wyłączną robi backend.
  auditLog(filtry: AuditLogFiltry): Observable<WpisDziennikaZdarzen[]> {
    let params = new HttpParams();
    if (filtry.typOperacji) params = params.set('typOperacji', filtry.typOperacji);
    if (filtry.od) params = params.set('od', toIsoDate(filtry.od));
    if (filtry.do) params = params.set('do', toIsoDate(filtry.do));
    return this.http.get<WpisDziennikaZdarzen[]>(`${this.baseUrl}/api/audit-log`, { params });
  }
}

// Formatuje z lokalnych składników daty (nie toISOString()) - unika przesunięcia doby przy
// strefach czasowych na wschód od UTC (lokalna północ "do dziś" nie staje się "do wczoraj").
function toIsoDate(data: Date): string {
  const rok = data.getFullYear();
  const miesiac = String(data.getMonth() + 1).padStart(2, '0');
  const dzien = String(data.getDate()).padStart(2, '0');
  return `${rok}-${miesiac}-${dzien}`;
}
