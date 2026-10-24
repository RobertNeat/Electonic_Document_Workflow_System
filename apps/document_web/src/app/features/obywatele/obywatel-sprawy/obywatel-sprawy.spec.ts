import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { NEVER, of, throwError } from 'rxjs';
import type {
  Obywatel,
  Sprawa,
  StronaWynikow,
} from '@electronic-document-workflow/document-shared-dto';
import { ObywatelSprawy } from './obywatel-sprawy';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

describe('ObywatelSprawy', () => {
  let apiStub: { szukaj: ReturnType<typeof vi.fn> };
  let kontekst: KartaObywatelaKontekst;

  const obywatel = { id: 'ob-1', typ: 'osoba_fizyczna' } as Obywatel;

  function strona(dane: Sprawa[]): StronaWynikow<Sprawa> {
    return { dane, strona: 0, rozmiarStrony: 200, liczbaWszystkich: dane.length };
  }

  beforeEach(() => {
    apiStub = { szukaj: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ObywatelSprawy],
      providers: [
        provideRouter([]),
        KartaObywatelaKontekst,
        { provide: SprawyApi, useValue: apiStub },
      ],
    });
    kontekst = TestBed.inject(KartaObywatelaKontekst);
  });

  it('wczytuje sprawy obywatela z rozmiarStrony=200, żeby uniknąć ucięcia listy', () => {
    const sprawy: Sprawa[] = [{ id: 's-1', znakSprawy: 'A.1.1.2026' } as Sprawa];
    apiStub.szukaj.mockReturnValue(of(strona(sprawy)));
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelSprawy);
    fixture.detectChanges();

    expect(apiStub.szukaj).toHaveBeenCalledWith({ obywatelId: 'ob-1', rozmiarStrony: 200 });
    expect(fixture.componentInstance['sprawy']()).toEqual(sprawy);
    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
    expect(fixture.componentInstance['komunikatBledu']()).toBeNull();
  });

  it('pokazuje stan "wczytywanie" odrębny od pustej listy, dopóki żądanie nie wróci', () => {
    apiStub.szukaj.mockReturnValue(NEVER);
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelSprawy);
    fixture.detectChanges();

    expect(fixture.componentInstance['wczytywanie']()).toBe(true);
    const tekst = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(tekst).not.toContain('Ten obywatel nie ma żadnych spraw.');
    expect(fixture.nativeElement.querySelector('#obywatel-sprawy-wczytywanie')).toBeTruthy();
  });

  it('pokazuje faktycznie pustą listę ("brak spraw"), gdy wczytywanie się zakończyło bez błędu', () => {
    apiStub.szukaj.mockReturnValue(of(strona([])));
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelSprawy);
    fixture.detectChanges();

    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
    expect(fixture.componentInstance['komunikatBledu']()).toBeNull();
    const tekst = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(tekst).toContain('Ten obywatel nie ma żadnych spraw.');
  });

  it('pokazuje zmapowany komunikat błędu HTTP, bez nadpisywania przez pustą listę', () => {
    apiStub.szukaj.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelSprawy);
    fixture.detectChanges();

    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
    expect(fixture.componentInstance['komunikatBledu']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
    expect(fixture.nativeElement.querySelector('#obywatel-sprawy-blad')).toBeTruthy();
  });
});
