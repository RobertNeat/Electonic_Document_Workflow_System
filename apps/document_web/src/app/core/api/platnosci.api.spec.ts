import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import type {
  Naleznosc,
  NaleznoscListElement,
  Wplata,
  WynikImportuCamt053,
} from '@electronic-document-workflow/document-shared-dto';
import { PlatnosciApi } from './platnosci.api';
import { environment } from '../../../environments/environment';

describe('PlatnosciApi', () => {
  let api: PlatnosciApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(PlatnosciApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('naleznosci() woła GET /api/naleznosci z obywatelId w query', () => {
    api.naleznosci({ obywatelId: 'ob-1' }).subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.documentApiBaseUrl}/api/naleznosci`,
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('obywatelId')).toBe('ob-1');

    req.flush([] as NaleznoscListElement[]);
  });

  it('naleznosci() woła GET /api/naleznosci z filtrem status w query', () => {
    api.naleznosci({ status: 'przeterminowana' }).subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.documentApiBaseUrl}/api/naleznosci`,
    );
    expect(req.request.params.get('status')).toBe('przeterminowana');
    expect(req.request.params.has('obywatelId')).toBe(false);

    req.flush([]);
  });

  it('naleznosci() bez filtrów nie wysyła żadnego parametru', () => {
    api.naleznosci({}).subscribe();

    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/naleznosci`);
    expect(req.request.params.has('status')).toBe(false);
    expect(req.request.params.has('obywatelId')).toBe(false);

    req.flush([]);
  });

  it('getById() woła GET /api/naleznosci/:id', () => {
    const naleznosc = { id: 'n-1' } as Naleznosc;
    api.getById('n-1').subscribe((wynik) => expect(wynik).toEqual(naleznosc));

    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/naleznosci/n-1`);
    expect(req.request.method).toBe('GET');
    req.flush(naleznosc);
  });

  it('wplaty() woła GET /api/wplaty z obywatelId w query', () => {
    api.wplaty({ obywatelId: 'ob-1' }).subscribe();

    const req = httpMock.expectOne((r) => r.url === `${environment.documentApiBaseUrl}/api/wplaty`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('obywatelId')).toBe('ob-1');

    req.flush([] as Wplata[]);
  });

  it('wplaty() woła GET /api/wplaty z filtrem naleznoscId w query', () => {
    api.wplaty({ naleznoscId: 'n-1' }).subscribe();

    const req = httpMock.expectOne((r) => r.url === `${environment.documentApiBaseUrl}/api/wplaty`);
    expect(req.request.params.get('naleznoscId')).toBe('n-1');
    expect(req.request.params.has('obywatelId')).toBe(false);

    req.flush([]);
  });

  it('dopasujWplate() woła POST /api/wplaty/:id/dopasuj z naleznoscId w body', () => {
    const wplata = { id: 'w-1', naleznoscId: 'n-1' } as Wplata;
    api.dopasujWplate('w-1', 'n-1').subscribe((wynik) => expect(wynik).toEqual(wplata));

    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/wplaty/w-1/dopasuj`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ naleznoscId: 'n-1' });
    req.flush(wplata);
  });

  it('importujCamt053() woła POST /api/import-camt053 z plikiem w polu "plik" multipart', () => {
    const plik = new File(['<Document></Document>'], 'wyciag.xml', { type: 'application/xml' });
    const wynik: WynikImportuCamt053 = {
      liczbaTransakcji: 2,
      liczbaDopasowanychAutomatycznie: 1,
      liczbaDoRekoncyliacji: 1,
      utworzoneWplaty: [],
    };
    api.importujCamt053(plik).subscribe((odpowiedz) => expect(odpowiedz).toEqual(wynik));

    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/import-camt053`);
    expect(req.request.method).toBe('POST');
    const body = req.request.body as FormData;
    expect(body instanceof FormData).toBe(true);
    const wyslanyPlik = body.get('plik') as File;
    expect(wyslanyPlik.name).toBe('wyciag.xml');
    req.flush(wynik);
  });
});
