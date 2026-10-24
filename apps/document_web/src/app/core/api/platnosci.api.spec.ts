import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import type {
  NaleznoscListElement,
  Wplata,
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

  it('wplaty() woła GET /api/wplaty z obywatelId w query', () => {
    api.wplaty({ obywatelId: 'ob-1' }).subscribe();

    const req = httpMock.expectOne((r) => r.url === `${environment.documentApiBaseUrl}/api/wplaty`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('obywatelId')).toBe('ob-1');

    req.flush([] as Wplata[]);
  });
});
