import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ObywateleApi } from './obywatele.api';
import { environment } from '../../../environments/environment';

describe('ObywateleApi', () => {
  let api: ObywateleApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ObywateleApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('szukaj() woła GET /api/obywatele z parametrem query, gdy podano zapytanie', () => {
    api.szukaj('Kowalski').subscribe();
    const req = httpMock.expectOne(
      (r) => r.url === `${environment.documentApiBaseUrl}/api/obywatele`,
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('query')).toBe('Kowalski');
    req.flush([]);
  });

  it('szukaj() bez zapytania nie wysyła parametru query', () => {
    api.szukaj().subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/obywatele`);
    expect(req.request.params.has('query')).toBe(false);
    req.flush([]);
  });

  it('szukaj("") nie wysyła parametru query (pusty string traktowany jak brak zapytania)', () => {
    api.szukaj('').subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/obywatele`);
    expect(req.request.params.has('query')).toBe(false);
    req.flush([]);
  });

  it('getById() woła GET /api/obywatele/:id', () => {
    api.getById('ob-1').subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/obywatele/ob-1`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });
});
