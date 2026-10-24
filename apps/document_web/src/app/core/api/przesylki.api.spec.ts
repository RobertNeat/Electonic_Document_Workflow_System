import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import type { Przesylka } from '@electronic-document-workflow/document-shared-dto';
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
});
