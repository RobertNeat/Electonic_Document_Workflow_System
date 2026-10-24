import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import type {
  Dokument,
  DokumentListElement,
  SzablonJasper,
  WersjaDokumentu,
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

  it('lista() bez filtrów nie wysyła żadnych query params', () => {
    api.lista().subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/dokumenty`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys().length).toBe(0);
    req.flush([] as DokumentListElement[]);
  });

  it('lista() z obywatelId woła GET /api/dokumenty z obywatelId w query', () => {
    api.lista({ obywatelId: 'ob-1' }).subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.documentApiBaseUrl}/api/dokumenty`,
    );
    expect(req.request.params.get('obywatelId')).toBe('ob-1');
    expect(req.request.params.has('sprawaId')).toBe(false);
    expect(req.request.params.has('typDokumentuId')).toBe(false);

    req.flush([] as DokumentListElement[]);
  });

  it('lista() z sprawaId i typDokumentuId woła GET /api/dokumenty z oboma w query', () => {
    api.lista({ sprawaId: 's-1', typDokumentuId: 'td-1' }).subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.documentApiBaseUrl}/api/dokumenty`,
    );
    expect(req.request.params.get('sprawaId')).toBe('s-1');
    expect(req.request.params.get('typDokumentuId')).toBe('td-1');
    expect(req.request.params.has('obywatelId')).toBe(false);

    req.flush([] as DokumentListElement[]);
  });

  it('getById() woła GET /api/dokumenty/:id', () => {
    const dokument = { id: 'd-1' } as Dokument;
    api.getById('d-1').subscribe((wynik) => expect(wynik).toEqual(dokument));

    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/dokumenty/d-1`);
    expect(req.request.method).toBe('GET');
    req.flush(dokument);
  });

  it('wersje() woła GET /api/dokumenty/:id/wersje', () => {
    api.wersje('d-1').subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/dokumenty/d-1/wersje`);
    expect(req.request.method).toBe('GET');
    req.flush([] as WersjaDokumentu[]);
  });

  it('szablony() bez filtru nie wysyła żadnych query params', () => {
    api.szablony().subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/szablony`);
    expect(req.request.params.keys().length).toBe(0);
    req.flush([] as SzablonJasper[]);
  });

  it('szablony(typDokumentuId) woła GET /api/szablony z typDokumentuId w query', () => {
    api.szablony('td-1').subscribe();
    const req = httpMock.expectOne(
      (r) => r.url === `${environment.documentApiBaseUrl}/api/szablony`,
    );
    expect(req.request.params.get('typDokumentuId')).toBe('td-1');
    req.flush([] as SzablonJasper[]);
  });

  it('historiaSzablonu() woła GET /api/szablony/:id/historia', () => {
    api.historiaSzablonu('sz-1').subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/szablony/sz-1/historia`);
    expect(req.request.method).toBe('GET');
    req.flush([] as SzablonJasper[]);
  });
});
