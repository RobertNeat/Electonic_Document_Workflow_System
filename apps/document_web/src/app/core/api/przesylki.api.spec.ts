import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import type {
  Przesylka,
  UtworzPrzesylkeRequest,
} from '@electronic-document-workflow/document-shared-dto';
import { PrzesylkiApi } from './przesylki.api';
import { environment } from '../../../environments/environment';

describe('PrzesylkiApi', () => {
  let api: PrzesylkiApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(PrzesylkiApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('szukaj() woła GET /api/przesylki z obywatelId w query', () => {
    api.szukaj({ obywatelId: 'ob-1' }).subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.documentApiBaseUrl}/api/przesylki`,
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('obywatelId')).toBe('ob-1');

    req.flush([] as Przesylka[]);
  });

  it('szukaj() bez obywatelId nie wysyła tego parametru', () => {
    api.szukaj({}).subscribe();

    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/przesylki`);
    expect(req.request.params.has('obywatelId')).toBe(false);

    req.flush([]);
  });

  it('szukaj() woła GET /api/przesylki z kierunkiem/statusem/kanałem w query', () => {
    api.szukaj({ kierunek: 'wychodzaca', status: 'nadane', kanal: 'email' }).subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.documentApiBaseUrl}/api/przesylki`,
    );
    expect(req.request.params.get('kierunek')).toBe('wychodzaca');
    expect(req.request.params.get('status')).toBe('nadane');
    expect(req.request.params.get('kanal')).toBe('email');

    req.flush([]);
  });

  it('getById() woła GET /api/przesylki/:id', () => {
    const przesylka = { id: 'p-1' } as Przesylka;
    api.getById('p-1').subscribe((wynik) => expect(wynik).toEqual(przesylka));

    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/przesylki/p-1`);
    expect(req.request.method).toBe('GET');
    req.flush(przesylka);
  });

  it('nadaj() woła POST /api/przesylki z body żądania', () => {
    const request: UtworzPrzesylkeRequest = {
      sprawaId: 's-1',
      dokumentId: 'd-1',
      obywatelId: 'ob-1',
      kanal: 'e-doreczenia',
    };
    const przesylka = { id: 'p-nowa' } as Przesylka;
    api.nadaj(request).subscribe((wynik) => expect(wynik).toEqual(przesylka));

    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/przesylki`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(przesylka);
  });
});
