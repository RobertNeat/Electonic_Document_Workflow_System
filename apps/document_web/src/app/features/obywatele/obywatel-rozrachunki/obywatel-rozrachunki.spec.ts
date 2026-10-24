import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localePl from '@angular/common/locales/pl';
import { NEVER, of, throwError } from 'rxjs';
import type {
  NaleznoscListElement,
  Obywatel,
  Wplata,
} from '@electronic-document-workflow/document-shared-dto';
import { ObywatelRozrachunki } from './obywatel-rozrachunki';
import { PlatnosciApi } from '../../../core/api/platnosci.api';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

// CurrencyPipe (template "Suma do zapłaty") wymaga zarejestrowanych danych locale "pl" - w
// aplikacji rejestrowane raz w app.config.ts, ale ten spec nie ładuje app.config.
registerLocaleData(localePl);

describe('ObywatelRozrachunki', () => {
  let apiStub: { naleznosci: ReturnType<typeof vi.fn>; wplaty: ReturnType<typeof vi.fn> };
  let kontekst: KartaObywatelaKontekst;

  const obywatel = { id: 'ob-1', typ: 'osoba_fizyczna' } as Obywatel;

  beforeEach(() => {
    apiStub = { naleznosci: vi.fn(), wplaty: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ObywatelRozrachunki],
      providers: [KartaObywatelaKontekst, { provide: PlatnosciApi, useValue: apiStub }],
    });
    kontekst = TestBed.inject(KartaObywatelaKontekst);
  });

  it('wczytuje należności i wpłaty obywatela przez PlatnosciApi z { obywatelId }', () => {
    const naleznosci: NaleznoscListElement[] = [
      {
        id: 'n-1',
        kwota: 100,
        kwotaRozliczona: 30,
        status: 'czesciowo_rozliczona',
      } as NaleznoscListElement,
    ];
    const wplaty: Wplata[] = [{ id: 'w-1', kwota: 30 } as Wplata];
    apiStub.naleznosci.mockReturnValue(of(naleznosci));
    apiStub.wplaty.mockReturnValue(of(wplaty));
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelRozrachunki);
    fixture.detectChanges();

    expect(apiStub.naleznosci).toHaveBeenCalledWith({ obywatelId: 'ob-1' });
    expect(apiStub.wplaty).toHaveBeenCalledWith({ obywatelId: 'ob-1' });
    expect(fixture.componentInstance['naleznosci']()).toEqual(naleznosci);
    expect(fixture.componentInstance['wplaty']()).toEqual(wplaty);
    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
    expect(fixture.componentInstance['komunikatBledu']()).toBeNull();
  });

  it('pokazuje stan "wczytywanie" odrębny od pustej listy, dopóki żądania nie wrócą', () => {
    apiStub.naleznosci.mockReturnValue(NEVER);
    apiStub.wplaty.mockReturnValue(NEVER);
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelRozrachunki);
    fixture.detectChanges();

    expect(fixture.componentInstance['wczytywanie']()).toBe(true);
    expect(fixture.nativeElement.querySelector('#obywatel-rozrachunki-wczytywanie')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('#obywatel-rozrachunki-podsumowanie')).toBeFalsy();
  });

  it('pokazuje faktycznie puste listy, gdy wczytywanie się zakończyło bez błędu', () => {
    apiStub.naleznosci.mockReturnValue(of([]));
    apiStub.wplaty.mockReturnValue(of([]));
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelRozrachunki);
    fixture.detectChanges();

    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
    expect(fixture.componentInstance['komunikatBledu']()).toBeNull();
    const tekst = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(tekst).toContain('Brak należności powiązanych z tym obywatelem.');
    expect(tekst).toContain('Brak wpłat powiązanych z tym obywatelem.');
  });

  it('pokazuje zmapowany komunikat błędu HTTP, gdy wołanie wpłat zawiedzie', () => {
    apiStub.naleznosci.mockReturnValue(of([]));
    apiStub.wplaty.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelRozrachunki);
    fixture.detectChanges();

    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
    expect(fixture.componentInstance['komunikatBledu']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
    expect(fixture.nativeElement.querySelector('#obywatel-rozrachunki-blad')).toBeTruthy();
    expect(fixture.componentInstance['naleznosci']()).toEqual([]);
  });

  it('sumaDoZaplaty() sumuje kwota - kwotaRozliczona po wszystkich należnościach', () => {
    const naleznosci: NaleznoscListElement[] = [
      {
        id: 'n-1',
        kwota: 100,
        kwotaRozliczona: 30,
        status: 'czesciowo_rozliczona',
      } as NaleznoscListElement,
      { id: 'n-2', kwota: 50, kwotaRozliczona: 50, status: 'rozliczona' } as NaleznoscListElement,
    ];
    apiStub.naleznosci.mockReturnValue(of(naleznosci));
    apiStub.wplaty.mockReturnValue(of([]));
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelRozrachunki);
    fixture.detectChanges();

    expect(fixture.componentInstance['sumaDoZaplaty']()).toBe(70);
  });
});
