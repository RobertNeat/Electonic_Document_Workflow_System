import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localePl from '@angular/common/locales/pl';
import type {
  NaleznoscListElement,
  Wplata,
} from '@electronic-document-workflow/document-shared-dto';
import { WplatyLista } from './wplaty-lista';
import { PlatnosciStore } from '../data/platnosci-store';
import { PlatnosciApi } from '../../../core/api/platnosci.api';

// CurrencyPipe w szablonie wymaga zarejestrowanych danych locale "pl" - w aplikacji rejestrowane
// raz w app.config.ts, ale ten spec nie ładuje app.config.
registerLocaleData(localePl);

describe('WplatyLista', () => {
  let storeStub: {
    wplaty: ReturnType<typeof vi.fn>;
    loading: ReturnType<typeof vi.fn>;
    blad: ReturnType<typeof vi.fn>;
    szukajWplat: ReturnType<typeof vi.fn>;
    dopasujWplate: ReturnType<typeof vi.fn>;
  };
  let apiStub: { naleznosci: ReturnType<typeof vi.fn> };

  const wplaty: Wplata[] = [
    {
      id: 'w-1',
      kwota: 50,
      data: '2026-01-10',
      tytul: 'Przelew',
      kontrahent: 'Anna Nowak',
      dopasowanaAutomatycznie: false,
    },
  ];

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
    {
      id: 'n-2',
      sprawaId: 's-2',
      obywatelId: 'ob-2',
      numerReferencyjny: 'REF-2',
      tytul: 'Mandat',
      kwota: 200,
      kwotaRozliczona: 200,
      terminPlatnosci: '2026-01-01',
      status: 'rozliczona',
      znakSprawy: 'OR.2.1.2026',
      obywatelNazwa: 'Piotr Wisniewski',
    },
  ];

  beforeEach(() => {
    storeStub = {
      wplaty: vi.fn().mockReturnValue(wplaty),
      loading: vi.fn().mockReturnValue(false),
      blad: vi.fn().mockReturnValue(null),
      szukajWplat: vi.fn(),
      dopasujWplate: vi.fn(),
    };
    apiStub = { naleznosci: vi.fn().mockReturnValue(of(naleznosci)) };

    TestBed.configureTestingModule({
      imports: [WplatyLista],
      providers: [
        provideRouter([]),
        { provide: PlatnosciStore, useValue: storeStub },
        { provide: PlatnosciApi, useValue: apiStub },
      ],
    });
  });

  it('woła store.szukajWplat() i PlatnosciApi.naleznosci() przy montowaniu', () => {
    const fixture = TestBed.createComponent(WplatyLista);
    fixture.detectChanges();

    expect(storeStub.szukajWplat).toHaveBeenCalledWith({});
    expect(apiStub.naleznosci).toHaveBeenCalledWith({});
  });

  it('opcjeNaleznosci zawiera tylko należności nierozliczone/czesciowo_rozliczone', async () => {
    const fixture = TestBed.createComponent(WplatyLista);
    fixture.detectChanges();
    await Promise.resolve();
    fixture.detectChanges();

    const opcje = fixture.componentInstance['opcjeNaleznosci']();
    expect(opcje).toHaveLength(1);
    expect(opcje[0].wartosc).toBe('n-1');
  });

  it('naleznoscTytul() rozwiązuje tytuł nawet dla należności w pełni rozliczonej', async () => {
    const fixture = TestBed.createComponent(WplatyLista);
    fixture.detectChanges();
    await Promise.resolve();
    fixture.detectChanges();

    expect(fixture.componentInstance.naleznoscTytul('n-2')).toBe('Mandat');
  });

  it('potwierdzDopasowanie() przy sukcesie zamyka dialog i odświeża wpłaty/należności', async () => {
    storeStub.dopasujWplate.mockResolvedValue({ id: 'w-1', naleznoscId: 'n-1' } as Wplata);

    const fixture = TestBed.createComponent(WplatyLista);
    fixture.detectChanges();
    await Promise.resolve();

    fixture.componentInstance.otworzDopasowanie(wplaty[0]);
    fixture.componentInstance['wybranaNaleznoscId'].set('n-1');
    storeStub.szukajWplat.mockClear();
    apiStub.naleznosci.mockClear();

    fixture.componentInstance.potwierdzDopasowanie();
    await Promise.resolve();
    await Promise.resolve();

    expect(storeStub.dopasujWplate).toHaveBeenCalledWith('w-1', 'n-1');
    expect(fixture.componentInstance['dialogWidoczny']()).toBe(false);
    expect(storeStub.szukajWplat).toHaveBeenCalled();
    expect(apiStub.naleznosci).toHaveBeenCalled();
  });

  it('potwierdzDopasowanie() przy błędzie pokazuje komunikat i NIE zamyka dialogu', async () => {
    storeStub.dopasujWplate.mockResolvedValue('Brak uprawnień do wykonania tej operacji.');

    const fixture = TestBed.createComponent(WplatyLista);
    fixture.detectChanges();
    await Promise.resolve();

    fixture.componentInstance.otworzDopasowanie(wplaty[0]);
    fixture.componentInstance['wybranaNaleznoscId'].set('n-1');

    fixture.componentInstance.potwierdzDopasowanie();
    await Promise.resolve();
    await Promise.resolve();

    expect(fixture.componentInstance['dialogWidoczny']()).toBe(true);
    expect(fixture.componentInstance['bladDopasowania']()).toBe(
      'Brak uprawnień do wykonania tej operacji.',
    );
  });

  it('błąd wczytania listy należności jest surowany w bladNaleznosci()', async () => {
    apiStub.naleznosci.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = TestBed.createComponent(WplatyLista);
    fixture.detectChanges();
    await Promise.resolve();
    fixture.detectChanges();

    expect(fixture.componentInstance['bladNaleznosci']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
  });
});
