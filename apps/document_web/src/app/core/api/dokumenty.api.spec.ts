import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import type {
  Dokument,
  DokumentListElement,
  DokumentObywatelaListElement,
} from '@electronic-document-workflow/document-shared-dto';
import { DokumentyApi } from './dokumenty.api';
import { environment } from '../../../environments/environment';

describe('DokumentyApi', () => {
  let api: DokumentyApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(DokumentyApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listaDlaObywatela() woła GET /api/dokumenty z obywatelId w query', () => {
    api.listaDlaObywatela('ob-1').subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.documentApiBaseUrl}/api/dokumenty`,
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('obywatelId')).toBe('ob-1');

    req.flush([] as DokumentObywatelaListElement[]);
  });

  it('listaDlaSprawy() woła GET /api/dokumenty z sprawaId w query', () => {
    api.listaDlaSprawy('s-1').subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.documentApiBaseUrl}/api/dokumenty`,
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('sprawaId')).toBe('s-1');

    req.flush([] as DokumentListElement[]);
  });

  it('getById() woła GET /api/dokumenty/:id', () => {
    const dokument = { id: 'd-1' } as Dokument;
    api.getById('d-1').subscribe((wynik) => expect(wynik).toEqual(dokument));

    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/dokumenty/d-1`);
    expect(req.request.method).toBe('GET');
    req.flush(dokument);
  });
});
