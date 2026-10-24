import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localePl from '@angular/common/locales/pl';
import { of, throwError } from 'rxjs';
import type {
  Naleznosc,
  Obywatel,
  Sprawa,
  Wplata,
} from '@electronic-document-workflow/document-shared-dto';
import { NaleznoscSzczegoly } from './naleznosc-szczegoly';
import { PlatnosciApi } from '../../../core/api/platnosci.api';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { ObywateleApi } from '../../../core/api/obywatele.api';

// CurrencyPipe w szablonie wymaga zarejestrowanych danych locale "pl" - w aplikacji rejestrowane
// raz w app.config.ts, ale ten spec nie ładuje app.config.
registerLocaleData(localePl);

describe('NaleznoscSzczegoly', () => {
  let platnosciApiStub: {
    getById: ReturnType<typeof vi.fn>;
    wplaty: ReturnType<typeof vi.fn>;
  };
  let sprawyApiStub: { getById: ReturnType<typeof vi.fn> };
  let obywateleApiStub: { getById: ReturnType<typeof vi.fn> };

  const naleznosc: Naleznosc = {
    id: 'n-1',
    sprawaId: 's-1',
    obywatelId: 'ob-1',
    numerReferencyjny: 'REF-1',
    tytul: 'Opłata',
    kwota: 100,
    kwotaRozliczona: 40,
    terminPlatnosci: '2026-01-01',
    status: 'czesciowo_rozliczona',
  };

  const wplaty: Wplata[] = [
    {
      id: 'w-1',
      naleznoscId: 'n-1',
      kwota: 40,
      data: '2026-01-05',
      tytul: 'Wpłata',
      kontrahent: 'Jan Kowalski',
      dopasowanaAutomatycznie: true,
    },
  ];

  beforeEach(() => {
    platnosciApiStub = {
      getById: vi.fn().mockReturnValue(of(naleznosc)),
      wplaty: vi.fn().mockReturnValue(of(wplaty)),
    };
    sprawyApiStub = { getById: vi.fn().mockReturnValue(of({ id: 's-1' } as Sprawa)) };
    obywateleApiStub = { getById: vi.fn().mockReturnValue(of({ id: 'ob-1' } as Obywatel)) };

    TestBed.configureTestingModule({
      imports: [NaleznoscSzczegoly],
      providers: [
        provideRouter([]),
        { provide: PlatnosciApi, useValue: platnosciApiStub },
        { provide: SprawyApi, useValue: sprawyApiStub },
        { provide: ObywateleApi, useValue: obywateleApiStub },
      ],
    });
  });

  it('wczytuje należność i powiązane wpłaty/sprawę/obywatela, wypełnia stan "ok"', async () => {
    const fixture = TestBed.createComponent(NaleznoscSzczegoly);
    fixture.componentRef.setInput('id', 'n-1');
    fixture.detectChanges();
    await Promise.resolve();
    await Promise.resolve();
    fixture.detectChanges();

    expect(fixture.componentInstance['status']()).toBe('ok');
    expect(fixture.componentInstance['naleznosc']()).toEqual(naleznosc);
    expect(fixture.componentInstance['wplaty']()).toEqual(wplaty);
    expect(platnosciApiStub.wplaty).toHaveBeenCalledWith({ naleznoscId: 'n-1' });
    expect(sprawyApiStub.getById).toHaveBeenCalledWith('s-1');
    expect(obywateleApiStub.getById).toHaveBeenCalledWith('ob-1');
  });

  it('404 na getById ustawia stan "nieznaleziona"', async () => {
    platnosciApiStub.getById.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );

    const fixture = TestBed.createComponent(NaleznoscSzczegoly);
    fixture.componentRef.setInput('id', 'n-brak');
    fixture.detectChanges();
    await Promise.resolve();
    await Promise.resolve();
    fixture.detectChanges();

    expect(fixture.componentInstance['status']()).toBe('nieznaleziona');
  });

  it('błąd sieci na getById ustawia stan "blad" z komunikatem', async () => {
    platnosciApiStub.getById.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0 })),
    );

    const fixture = TestBed.createComponent(NaleznoscSzczegoly);
    fixture.componentRef.setInput('id', 'n-1');
    fixture.detectChanges();
    await Promise.resolve();
    await Promise.resolve();
    fixture.detectChanges();

    expect(fixture.componentInstance['status']()).toBe('blad');
    expect(fixture.componentInstance['komunikatBledu']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
  });

  it('błąd wywołania wpłat nie przełącza stanu na "nieznaleziona" - pokazuje puste wpłaty', async () => {
    platnosciApiStub.wplaty.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = TestBed.createComponent(NaleznoscSzczegoly);
    fixture.componentRef.setInput('id', 'n-1');
    fixture.detectChanges();
    await Promise.resolve();
    await Promise.resolve();
    fixture.detectChanges();

    expect(fixture.componentInstance['status']()).toBe('ok');
    expect(fixture.componentInstance['wplaty']()).toEqual([]);
  });
});
