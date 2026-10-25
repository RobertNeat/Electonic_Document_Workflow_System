import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  WpisHistoriiWymiany,
  WynikWalidacjiUbl,
  WynikWalidacjiXml,
} from '@electronic-document-workflow/document-shared-dto';
import { environment } from '../../../environments/environment';

/**
 * Klient HTTP dla endpointów modułu Wymiana EDI/XML w `document_api` - import komunikatu sprawy
 * XML (walidacja XSD + mapowanie na Sprawa), import faktury kosztowej UBL/PEPPOL (walidacja +
 * mapowanie na FakturaUbl) oraz historia obu wymian. Import jest tylko podglądowy - żadna
 * Sprawa/Naleznosc nie jest tworzona automatycznie, odpowiedź 200 z sukces=false to normalny
 * wynik walidacji semantycznej, nie błąd HTTP. Eksport nie jest zaimplementowany backendowo.
 */
@Injectable({ providedIn: 'root' })
export class WymianaApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.documentApiBaseUrl;

  importujXml(plik: File): Observable<WynikWalidacjiXml> {
    const formData = new FormData();
    formData.append('plik', plik);
    return this.http.post<WynikWalidacjiXml>(`${this.baseUrl}/api/wymiana/xml`, formData);
  }

  importujUbl(plik: File): Observable<WynikWalidacjiUbl> {
    const formData = new FormData();
    formData.append('plik', plik);
    return this.http.post<WynikWalidacjiUbl>(`${this.baseUrl}/api/wymiana/ubl`, formData);
  }

  historiaXml(): Observable<WpisHistoriiWymiany[]> {
    return this.http.get<WpisHistoriiWymiany[]>(`${this.baseUrl}/api/wymiana/xml/historia`);
  }

  historiaUbl(): Observable<WpisHistoriiWymiany[]> {
    return this.http.get<WpisHistoriiWymiany[]>(`${this.baseUrl}/api/wymiana/ubl/historia`);
  }
}
