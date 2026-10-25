import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import type {
  WpisHistoriiWymiany,
  WynikWalidacjiUbl,
  WynikWalidacjiXml,
} from '@electronic-document-workflow/document-shared-dto';
import { WymianaApi } from './wymiana.api';
import { environment } from '../../../environments/environment';

describe('WymianaApi', () => {
  let api: WymianaApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(WymianaApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('importujXml() woła POST /api/wymiana/xml z plikiem w polu "plik" multipart', () => {
    const plik = new File(['<komunikat/>'], 'komunikat.xml', { type: 'application/xml' });
    const wynik: WynikWalidacjiXml = { sukces: true, bledy: [] };
    api.importujXml(plik).subscribe((odpowiedz) => expect(odpowiedz).toEqual(wynik));

    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/wymiana/xml`);
    expect(req.request.method).toBe('POST');
    const body = req.request.body as FormData;
    expect(body instanceof FormData).toBe(true);
    expect((body.get('plik') as File).name).toBe('komunikat.xml');
    req.flush(wynik);
  });

  it('importujXml() przekazuje odpowiedź 200 z sukces=false (błąd walidacji semantycznej)', () => {
    const plik = new File(['<komunikat/>'], 'niepoprawny.xml');
    const wynik: WynikWalidacjiXml = {
      sukces: false,
      bledy: [{ element: 'znakSprawy', opis: 'Element wymagany' }],
    };
    api.importujXml(plik).subscribe((odpowiedz) => expect(odpowiedz).toEqual(wynik));

    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/wymiana/xml`);
    req.flush(wynik);
  });

  it('importujUbl() woła POST /api/wymiana/ubl z plikiem w polu "plik" multipart', () => {
    const plik = new File(['<Invoice/>'], 'faktura.xml');
    const wynik: WynikWalidacjiUbl = { sukces: true, bledy: [] };
    api.importujUbl(plik).subscribe((odpowiedz) => expect(odpowiedz).toEqual(wynik));

    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/wymiana/ubl`);
    expect(req.request.method).toBe('POST');
    const body = req.request.body as FormData;
    expect((body.get('plik') as File).name).toBe('faktura.xml');
    req.flush(wynik);
  });

  it('historiaXml() woła GET /api/wymiana/xml/historia', () => {
    api.historiaXml().subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/wymiana/xml/historia`);
    expect(req.request.method).toBe('GET');
    req.flush([] as WpisHistoriiWymiany[]);
  });

  it('historiaUbl() woła GET /api/wymiana/ubl/historia', () => {
    api.historiaUbl().subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/wymiana/ubl/historia`);
    expect(req.request.method).toBe('GET');
    req.flush([] as WpisHistoriiWymiany[]);
  });
});
