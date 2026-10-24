import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import localePl from '@angular/common/locales/pl';
import type { NaleznoscListElement } from '@electronic-document-workflow/document-shared-dto';
import { PlatnosciStrona } from './platnosci-strona';
import { PlatnosciStore } from '../data/platnosci-store';

// CurrencyPipe w szablonie wymaga zarejestrowanych danych locale "pl" - w aplikacji rejestrowane
// raz w app.config.ts, ale ten spec nie ładuje app.config.
registerLocaleData(localePl);

describe('PlatnosciStrona', () => {
  let storeStub: {
    naleznosci: ReturnType<typeof vi.fn>;
    loading: ReturnType<typeof vi.fn>;
    blad: ReturnType<typeof vi.fn>;
    szukajNaleznosci: ReturnType<typeof vi.fn>;
  };

  const naleznosci: NaleznoscListElement[] = [
    {
      id: 'n-1',
      sprawaId: 's-1',
      obywatelId: 'ob-1',
      numerReferencyjny: 'REF-1',
      tytul: 'Opłata',
      kwota: 100,
      kwotaRozliczona: 0,
      terminPlatnosci: '2026-01-01',
      status: 'nierozliczona',
      znakSprawy: 'OR.1.1.2026',
      obywatelNazwa: 'Jan Kowalski',
    },
  ];

  beforeEach(() => {
    storeStub = {
      naleznosci: vi.fn().mockReturnValue(naleznosci),
      loading: vi.fn().mockReturnValue(false),
      blad: vi.fn().mockReturnValue(null),
      szukajNaleznosci: vi.fn(),
    };

    TestBed.configureTestingModule({
      imports: [PlatnosciStrona],
      providers: [provideRouter([]), { provide: PlatnosciStore, useValue: storeStub }],
    });
  });

  it('woła store.szukajNaleznosci() z filtrem statusu przy montowaniu', () => {
    const fixture = TestBed.createComponent(PlatnosciStrona);
    fixture.detectChanges();

    expect(storeStub.szukajNaleznosci).toHaveBeenCalledWith({ status: null });
  });

  it('zmiana filtra statusu wywołuje ponowne szukajNaleznosci()', () => {
    const fixture = TestBed.createComponent(PlatnosciStrona);
    fixture.detectChanges();
    storeStub.szukajNaleznosci.mockClear();

    fixture.componentInstance['filtrStatusu'].set('przeterminowana');
    fixture.detectChanges();

    expect(storeStub.szukajNaleznosci).toHaveBeenCalledWith({ status: 'przeterminowana' });
  });

  it('wyświetla znakSprawy/obywatelNazwa zwrócone wprost przez API (NaleznoscListElement)', () => {
    const fixture = TestBed.createComponent(PlatnosciStrona);
    fixture.detectChanges();

    const tekst = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(tekst).toContain('OR.1.1.2026');
    expect(tekst).toContain('Jan Kowalski');
  });

  it('rozróżnia stan wczytywania od pustej listy', () => {
    storeStub.loading.mockReturnValue(true);
    storeStub.naleznosci.mockReturnValue([]);

    const fixture = TestBed.createComponent(PlatnosciStrona);
    fixture.detectChanges();

    expect(fixture.componentInstance['loading']()).toBe(true);
    expect(fixture.componentInstance['naleznosci']()).toEqual([]);
  });

  it('pokazuje komunikat błędu HTTP ze store.blad()', () => {
    storeStub.blad.mockReturnValue('Brak połączenia z serwerem (sieć lub CORS).');

    const fixture = TestBed.createComponent(PlatnosciStrona);
    fixture.detectChanges();

    const tekst = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(tekst).toContain('Brak połączenia z serwerem');
  });
});
