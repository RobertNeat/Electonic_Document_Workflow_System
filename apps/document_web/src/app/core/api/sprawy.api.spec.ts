import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import type { Sprawa, StronaWynikow } from '@electronic-document-workflow/document-shared-dto';
import { SprawyApi } from './sprawy.api';
import { environment } from '../../../environments/environment';

describe('SprawyApi', () => {
  let api: SprawyApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(SprawyApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('szukaj() woła GET /api/sprawy tylko z podanymi filtrami (brak null/undefined w query)', () => {
    api
      .szukaj({ stan: 'W toku', wydzial: null, q: undefined, strona: 0, rozmiarStrony: 20 })
      .subscribe();

    const req = httpMock.expectOne((r) => r.url === `${environment.documentApiBaseUrl}/api/sprawy`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('stan')).toBe('W toku');
    expect(req.request.params.has('wydzial')).toBe(false);
    expect(req.request.params.has('q')).toBe(false);
    expect(req.request.params.get('strona')).toBe('0');
    expect(req.request.params.get('rozmiarStrony')).toBe('20');

    req.flush({
      dane: [],
      strona: 0,
      rozmiarStrony: 20,
      liczbaWszystkich: 0,
    } as StronaWynikow<Sprawa>);
  });

  it('szukaj() bez filtrów nie wysyła żadnych query params', () => {
    api.szukaj({}).subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/sprawy`);
    expect(req.request.params.keys().length).toBe(0);
    req.flush({ dane: [], strona: 0, rozmiarStrony: 20, liczbaWszystkich: 0 });
  });

  it('getById() woła GET /api/sprawy/:id', () => {
    api.getById('spr-1').subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/sprawy/spr-1`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('rejestrujSprawe() woła POST /api/sprawy z body żądania', () => {
    const request = {
      definicjaObieguId: 'obieg-decyzja',
      jrwa: '6220',
      kategoriaArchiwalna: 'B10' as const,
      tytul: 'Tytuł',
      obywatelId: 'ob-1',
      wydzial: 'Wydział Organizacyjny',
      referent: 'Jan Referent',
    };
    api.rejestrujSprawe(request).subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/sprawy`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({});
  });

  it('zmienStan() woła PATCH /api/sprawy/:id/stan z { nowyStan }', () => {
    api.zmienStan('spr-1', 'W toku').subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/sprawy/spr-1/stan`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ nowyStan: 'W toku' });
    req.flush({});
  });

  it('metryka() woła GET /api/sprawy/:id/metryka', () => {
    api.metryka('spr-1').subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/sprawy/spr-1/metryka`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('definicjeObiegow() woła GET /api/definicje-obiegow', () => {
    api.definicjeObiegow().subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/definicje-obiegow`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('jrwa() woła GET /api/slowniki/jrwa', () => {
    api.jrwa().subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/slowniki/jrwa`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('typyDokumentow() woła GET /api/slowniki/typy-dokumentow', () => {
    api.typyDokumentow().subscribe();
    const req = httpMock.expectOne(
      `${environment.documentApiBaseUrl}/api/slowniki/typy-dokumentow`,
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
