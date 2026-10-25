import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  StatusAkcjiZbiorowej,
  UruchomAkcjeZbiorowaRequest,
  UruchomAkcjeZbiorowaResponse,
} from '@electronic-document-workflow/document-shared-dto';
import { environment } from '../../../environments/environment';

/**
 * Klient HTTP dla jednego generycznego endpointu akcji zbiorowych w `document_api` - żądanie jest
 * tylko publikowane do RabbitMQ (202 Accepted), przetwarzanie per-element jest asynchroniczne.
 * Postęp odpytywany pollingiem przez status() - brak WebSocket/SSE po stronie backendu.
 */
@Injectable({ providedIn: 'root' })
export class AkcjeZbioroweApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.documentApiBaseUrl;

  uruchom(request: UruchomAkcjeZbiorowaRequest): Observable<UruchomAkcjeZbiorowaResponse> {
    return this.http.post<UruchomAkcjeZbiorowaResponse>(
      `${this.baseUrl}/api/akcje-zbiorowe`,
      request,
    );
  }

  status(akcjaId: string): Observable<StatusAkcjiZbiorowej> {
    return this.http.get<StatusAkcjiZbiorowej>(
      `${this.baseUrl}/api/akcje-zbiorowe/${akcjaId}/status`,
    );
  }
}
