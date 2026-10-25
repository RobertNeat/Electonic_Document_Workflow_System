import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdministracjaApi } from './administracja.api';
import { environment } from '../../../environments/environment';

describe('AdministracjaApi', () => {
  let api: AdministracjaApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(AdministracjaApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('uzytkownicy() woła GET /api/uzytkownicy tylko z podanymi filtrami', () => {
    api.uzytkownicy('Wydział Organizacyjny', 'administrator').subscribe();
    const req = httpMock.expectOne(
      (r) => r.url === `${environment.documentApiBaseUrl}/api/uzytkownicy`,
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('wydzial')).toBe('Wydział Organizacyjny');
    expect(req.request.params.get('rola')).toBe('administrator');
    req.flush([]);
  });

  it('uzytkownicy() bez filtrów nie wysyła żadnych query params', () => {
    api.uzytkownicy().subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/uzytkownicy`);
    expect(req.request.params.keys().length).toBe(0);
    req.flush([]);
  });

  it('utworzUzytkownika() woła POST /api/uzytkownicy z body żądania', () => {
    const dane = {
      login: 'jan.kowalski',
      imie: 'Jan',
      nazwisko: 'Kowalski',
      email: 'jan.kowalski@urzad.gov.pl',
      role: ['referent' as const],
      wydzial: 'Wydział Organizacyjny',
      stanowisko: 'Referent',
    };
    api.utworzUzytkownika(dane).subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/uzytkownicy`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dane);
    req.flush({});
  });

  it('aktualizujRoleUzytkownika() woła PUT /api/uzytkownicy/:id/role z { role }', () => {
    api.aktualizujRoleUzytkownika('u-1', ['administrator']).subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/uzytkownicy/u-1/role`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ role: ['administrator'] });
    req.flush({});
  });

  it('strukturaOrganizacyjna() woła GET /api/struktura-organizacyjna', () => {
    api.strukturaOrganizacyjna().subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/struktura-organizacyjna`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('dostawcyWysylki() woła GET /api/slowniki/dostawcy-wysylki', () => {
    api.dostawcyWysylki().subscribe();
    const req = httpMock.expectOne(
      `${environment.documentApiBaseUrl}/api/slowniki/dostawcy-wysylki`,
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('aktualizujDostawceWysylki() woła PUT /api/slowniki/dostawcy-wysylki/:id z body żądania', () => {
    const dane = { id: 'd-1', nazwa: 'e-Doręczenia', typ: 'e-doreczenia' as const, aktywny: true };
    api.aktualizujDostawceWysylki('d-1', dane).subscribe();
    const req = httpMock.expectOne(
      `${environment.documentApiBaseUrl}/api/slowniki/dostawcy-wysylki/d-1`,
    );
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(dane);
    req.flush({});
  });

  it('stawkiOplat() woła GET /api/slowniki/stawki-oplat', () => {
    api.stawkiOplat().subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/slowniki/stawki-oplat`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('aktualizujStawkeOplaty() woła PUT /api/slowniki/stawki-oplat/:id z body żądania', () => {
    const dane = { id: 's-1', nazwa: 'Opłata skarbowa', kwota: 17 };
    api.aktualizujStawkeOplaty('s-1', dane).subscribe();
    const req = httpMock.expectOne(
      `${environment.documentApiBaseUrl}/api/slowniki/stawki-oplat/s-1`,
    );
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(dane);
    req.flush({});
  });

  it('auditLog() woła GET /api/audit-log z typOperacji i zakresem dat jako ISO yyyy-MM-dd', () => {
    api
      .auditLog({
        typOperacji: 'Rejestracja sprawy',
        od: new Date(2026, 9, 1),
        do: new Date(2026, 9, 20),
      })
      .subscribe();
    const req = httpMock.expectOne(
      (r) => r.url === `${environment.documentApiBaseUrl}/api/audit-log`,
    );
    expect(req.request.params.get('typOperacji')).toBe('Rejestracja sprawy');
    expect(req.request.params.get('od')).toBe('2026-10-01');
    expect(req.request.params.get('do')).toBe('2026-10-20');
    req.flush([]);
  });

  it('auditLog() bez filtrów nie wysyła żadnych query params', () => {
    api.auditLog({}).subscribe();
    const req = httpMock.expectOne(`${environment.documentApiBaseUrl}/api/audit-log`);
    expect(req.request.params.keys().length).toBe(0);
    req.flush([]);
  });
});
