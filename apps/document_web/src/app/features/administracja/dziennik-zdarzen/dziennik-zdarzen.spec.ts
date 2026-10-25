import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import type { WpisDziennikaZdarzen } from '@electronic-document-workflow/document-shared-dto';
import { DziennikZdarzen } from './dziennik-zdarzen';
import { AdministracjaApi } from '../../../core/api/administracja.api';

describe('DziennikZdarzen', () => {
  let apiStub: { auditLog: ReturnType<typeof vi.fn> };

  const wpisy: WpisDziennikaZdarzen[] = [
    {
      id: 'ev-1',
      dataCzas: '2026-10-20T08:12:00+02:00',
      uzytkownik: 'demo.kancelaria',
      typOperacji: 'Rejestracja sprawy',
      obiekt: 'WO.6220.14.2026',
      szczegoly: 'Zarejestrowano wniosek.',
    },
  ];

  beforeEach(() => {
    apiStub = { auditLog: vi.fn().mockReturnValue(of(wpisy)) };
    TestBed.configureTestingModule({
      imports: [DziennikZdarzen],
      providers: [{ provide: AdministracjaApi, useValue: apiStub }],
    });
  });

  it('wczytuje wpisy dziennika przy montowaniu bez filtrów', () => {
    const fixture = TestBed.createComponent(DziennikZdarzen);
    fixture.detectChanges();

    expect(apiStub.auditLog).toHaveBeenCalledWith({ typOperacji: null, od: null, do: null });
    expect(fixture.componentInstance.wpisy()).toEqual(wpisy);
    expect(fixture.componentInstance.wczytywanie()).toBe(false);
  });

  it('zmienFiltrTypu() wywołuje auditLog() z nowym typem operacji', () => {
    const fixture = TestBed.createComponent(DziennikZdarzen);
    fixture.detectChanges();
    apiStub.auditLog.mockClear();

    fixture.componentInstance.zmienFiltrTypu('Rejestracja wpłaty');

    expect(apiStub.auditLog).toHaveBeenCalledWith({
      typOperacji: 'Rejestracja wpłaty',
      od: null,
      do: null,
    });
  });

  it('zmienFiltrZakresDat() wywołuje auditLog() z zakresem dat, do = od gdy druga data nie wybrana', () => {
    const fixture = TestBed.createComponent(DziennikZdarzen);
    fixture.detectChanges();
    apiStub.auditLog.mockClear();

    const od = new Date(2026, 9, 1);
    fixture.componentInstance.zmienFiltrZakresDat([od, null as unknown as Date]);

    expect(apiStub.auditLog).toHaveBeenCalledWith({ typOperacji: null, od, do: od });
  });

  it('wyczyscFiltry() resetuje filtry i odświeża listę', () => {
    const fixture = TestBed.createComponent(DziennikZdarzen);
    fixture.detectChanges();
    fixture.componentInstance.zmienFiltrTypu('Dekretacja');
    apiStub.auditLog.mockClear();

    fixture.componentInstance.wyczyscFiltry();

    expect(fixture.componentInstance.filtrTypu()).toBeNull();
    expect(fixture.componentInstance.filtrZakresDat()).toBeNull();
    expect(apiStub.auditLog).toHaveBeenCalledWith({ typOperacji: null, od: null, do: null });
  });

  it('pokazuje komunikat błędu, gdy auditLog() zawiedzie', () => {
    apiStub.auditLog.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = TestBed.createComponent(DziennikZdarzen);
    fixture.detectChanges();

    expect(fixture.componentInstance.komunikatBledu()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
    expect(fixture.componentInstance.wpisy()).toEqual([]);
  });

  it('zawiera pełną, zahardkodowaną listę 8 typów operacji (nie wyliczoną z danych)', () => {
    const fixture = TestBed.createComponent(DziennikZdarzen);
    fixture.detectChanges();

    expect(fixture.componentInstance.typyOperacji).toHaveLength(8);
    expect(fixture.componentInstance.typyOperacji).toContain('Wygenerowanie wezwania do zapłaty');
  });
});
