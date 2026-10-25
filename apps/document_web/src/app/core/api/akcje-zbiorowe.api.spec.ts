import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import type {
  StatusAkcjiZbiorowej,
  UruchomAkcjeZbiorowaRequest,
  UruchomAkcjeZbiorowaResponse,
} from '@electronic-document-workflow/document-shared-dto';
import { AkcjeZbioroweApi } from './akcje-zbiorowe.api';
import { environment } from '../../../environments/environment';

describe('AkcjeZbioroweApi', () => {
  let api: AkcjeZbioroweApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(AkcjeZbioroweApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('uruchom() woła POST /api/akcje-zbiorowe z żądaniem w body', () => {
    const request: UruchomAkcjeZbiorowaRequest<'ZMIANA_STANU'> = {
      typAkcji: 'ZMIANA_STANU',
      typCelu: 'SPRAWA',
      idCelow: ['s-1', 's-2'],
      parametry: { nowyStan: 'W toku' },
    };
    const response: UruchomAkcjeZbiorowaResponse = { akcjaId: 'akcja-1' };
    api.uruchom(request).subscribe((wynik) => expect(wynik).toEqual(response));

    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/akcje-zbiorowe`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response);
  });

  it('status() woła GET /api/akcje-zbiorowe/:akcjaId/status', () => {
    const status: StatusAkcjiZbiorowej = {
      akcjaId: 'akcja-1',
      typAkcji: 'ZMIANA_STANU',
      typCelu: 'SPRAWA',
      status: 'trwajaca',
      postep: 50,
      lacznie: 2,
      przetworzone: 1,
      bledne: 0,
      wynikiElementow: [],
    };
    api.status('akcja-1').subscribe((wynik) => expect(wynik).toEqual(status));

    const req = httpMock.expectOne(
      `${environment.documentApiBaseUrl}/api/akcje-zbiorowe/akcja-1/status`,
    );
    expect(req.request.method).toBe('GET');
    req.flush(status);
  });
});
