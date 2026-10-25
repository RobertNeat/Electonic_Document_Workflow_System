import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import type {
  NaleznoscListElement,
  Obywatel,
  Przesylka,
  Sprawa,
  StronaWynikow,
  Uzytkownik,
} from '@electronic-document-workflow/document-shared-dto';
import { PulpitStrona } from './pulpit-strona';
import { AuthService } from '../../../core/services/auth.service';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { PlatnosciApi } from '../../../core/api/platnosci.api';
import { PrzesylkiApi } from '../../../core/api/przesylki.api';
import { ObywateleApi } from '../../../core/api/obywatele.api';

describe('PulpitStrona', () => {
  let authStub: { uzytkownik: ReturnType<typeof vi.fn> };
  let sprawyApiStub: { szukaj: ReturnType<typeof vi.fn> };
  let platnosciApiStub: { naleznosci: ReturnType<typeof vi.fn> };
  let przesylkiApiStub: { szukaj: ReturnType<typeof vi.fn> };
  let obywateleApiStub: { getById: ReturnType<typeof vi.fn> };

  const uzytkownik: Uzytkownik = {
    id: 'u-1',
    login: 'kzielinska',
    imie: 'Katarzyna',
    nazwisko: 'Zielińska',
    email: 'kzielinska@urzad.gov.pl',
    role: ['referent'],
    wydzial: '',
    stanowisko: '',
  };

  function dataWzgledna(dni: number): string {
    const d = new Date();
    d.setDate(d.getDate() + dni);
    return d.toISOString().slice(0, 10);
  }

  const sprawyBaza: Sprawa[] = [
    // sprawa referenta, najnowsza - przyblizonyWydzial ma być z tej sprawy
    {
      id: 'spr-p3',
      znakSprawy: 'WO.6220.23.2026',
      tytul: 'Przebudowa drogi',
      jrwa: '6220',
      definicjaObieguId: 'obieg-decyzja',
      stan: 'W toku',
      obywatelId: 'ob-1',
      wydzial: 'Wydział Ochrony Środowiska',
      referent: 'Katarzyna Zielińska',
      dataWszczecia: dataWzgledna(-5),
      terminUstawowy: dataWzgledna(20),
      kategoriaArchiwalna: 'A',
      zamknieta: false,
    },
    // sprawa referenta, przeterminowany termin ustawowy -> krytyczny
    {
      id: 'spr-p1',
      znakSprawy: 'WO.6220.21.2026',
      tytul: 'Budowa hali magazynowej',
      jrwa: '6220',
      definicjaObieguId: 'obieg-decyzja',
      stan: 'W toku',
      obywatelId: 'ob-3',
      wydzial: 'Wydział Ochrony Środowiska',
      referent: 'Katarzyna Zielińska',
      dataWszczecia: dataWzgledna(-30),
      terminUstawowy: dataWzgledna(-3),
      kategoriaArchiwalna: 'A',
      zamknieta: false,
    },
    // sprawa referenta, termin ustawowy za 4 dni -> krytyczny (<=7)
    {
      id: 'spr-p2',
      znakSprawy: 'WO.6220.22.2026',
      tytul: 'Stacja paliw',
      jrwa: '6220',
      definicjaObieguId: 'obieg-decyzja',
      stan: 'Do akceptacji',
      obywatelId: 'ob-2',
      wydzial: 'Wydział Ochrony Środowiska',
      referent: 'Katarzyna Zielińska',
      dataWszczecia: dataWzgledna(-14),
      terminUstawowy: dataWzgledna(4),
      kategoriaArchiwalna: 'A',
      zamknieta: false,
    },
    // sprawa referenta, ale zamknięta - nie liczy się do "moje sprawy"
    {
      id: 'spr-zamknieta',
      znakSprawy: 'WO.6220.9.2026',
      tytul: 'Sprawa zamknięta',
      jrwa: '6220',
      definicjaObieguId: 'obieg-decyzja',
      stan: 'Zakończona',
      obywatelId: 'ob-1',
      wydzial: 'Wydział Ochrony Środowiska',
      referent: 'Katarzyna Zielińska',
      dataWszczecia: dataWzgledna(-60),
      kategoriaArchiwalna: 'A',
      zamknieta: true,
    },
    // sprawa referenta, ale innego wydziału - nie powinna wpływać na join po przybliżonym wydziale
    {
      id: 'spr-inny-wydzial',
      znakSprawy: 'OR.1.2026',
      tytul: 'Sprawa innego wydziału',
      jrwa: '1510',
      definicjaObieguId: 'obieg-skarga',
      stan: 'W toku',
      obywatelId: 'ob-1',
      wydzial: 'Wydział Organizacyjny',
      referent: 'Katarzyna Zielińska',
      dataWszczecia: dataWzgledna(-40),
      kategoriaArchiwalna: 'B5',
      zamknieta: false,
    },
    // sprawa innego referenta
    {
      id: 'spr-2',
      znakSprawy: 'OR.1510.3.2026',
      tytul: 'Skarga',
      jrwa: '1510',
      definicjaObieguId: 'obieg-skarga',
      stan: 'Odpowiedź przygotowana',
      obywatelId: 'ob-2',
      wydzial: 'Wydział Organizacyjny',
      referent: 'Piotr Lewandowski',
      dataWszczecia: dataWzgledna(-2),
      kategoriaArchiwalna: 'B5',
      zamknieta: false,
    },
  ];

  const stronaSprawy: StronaWynikow<Sprawa> = {
    dane: sprawyBaza,
    strona: 0,
    rozmiarStrony: 200,
    liczbaWszystkich: sprawyBaza.length,
  };

  const naleznosci: NaleznoscListElement[] = [
    {
      id: 'n-1',
      sprawaId: 'spr-p3',
      obywatelId: 'ob-1',
      numerReferencyjny: 'REF-1',
      tytul: 'Opłata',
      kwota: 100,
      kwotaRozliczona: 0,
      terminPlatnosci: dataWzgledna(10),
      status: 'nierozliczona',
      znakSprawy: 'WO.6220.23.2026',
      obywatelNazwa: 'Jan Kowalski',
    },
    {
      id: 'n-2',
      sprawaId: 'spr-p3',
      obywatelId: 'ob-1',
      numerReferencyjny: 'REF-2',
      tytul: 'Opłata rozliczona',
      kwota: 50,
      kwotaRozliczona: 50,
      terminPlatnosci: dataWzgledna(5),
      status: 'rozliczona',
      znakSprawy: 'WO.6220.23.2026',
      obywatelNazwa: 'Jan Kowalski',
    },
    {
      id: 'n-3',
      sprawaId: 'spr-2',
      obywatelId: 'ob-2',
      numerReferencyjny: 'REF-3',
      tytul: 'Opłata innego wydziału',
      kwota: 20,
      kwotaRozliczona: 0,
      terminPlatnosci: dataWzgledna(10),
      status: 'nierozliczona',
      znakSprawy: 'OR.1510.3.2026',
      obywatelNazwa: 'Anna Nowak',
    },
  ];

  const przesylki: Przesylka[] = [
    {
      id: 'p-1',
      sprawaId: 'spr-p3',
      dokumentId: 'd-1',
      obywatelId: 'ob-1',
      kanal: 'operator_pocztowy',
      status: 'nadane',
      numerReferencyjny: 'PR-1',
      dataNadania: dataWzgledna(-1),
      dataStatusu: dataWzgledna(-1),
      kierunek: 'wychodzaca',
      historiaStatusow: [],
    },
    {
      id: 'p-2',
      sprawaId: 'spr-p3',
      dokumentId: 'd-2',
      obywatelId: 'ob-1',
      kanal: 'operator_pocztowy',
      status: 'doreczone',
      numerReferencyjny: 'PR-2',
      dataNadania: dataWzgledna(-5),
      dataStatusu: dataWzgledna(-1),
      kierunek: 'wychodzaca',
      historiaStatusow: [],
    },
    {
      id: 'p-3',
      sprawaId: 'spr-2',
      dokumentId: 'd-3',
      obywatelId: 'ob-2',
      kanal: 'email',
      status: 'awizo',
      numerReferencyjny: 'PR-3',
      dataNadania: dataWzgledna(-2),
      dataStatusu: dataWzgledna(-1),
      kierunek: 'wychodzaca',
      historiaStatusow: [],
    },
  ];

  const obywatel1: Obywatel = {
    id: 'ob-1',
    typ: 'osoba_fizyczna',
    imie: 'Jan',
    nazwisko: 'Kowalski',
    adres: {
      ulica: 'Testowa',
      numerDomu: '1',
      kodPocztowy: '00-001',
      miejscowosc: 'Warszawa',
    },
  };

  beforeEach(() => {
    authStub = { uzytkownik: vi.fn().mockReturnValue(uzytkownik) };
    sprawyApiStub = { szukaj: vi.fn().mockReturnValue(of(stronaSprawy)) };
    platnosciApiStub = { naleznosci: vi.fn().mockReturnValue(of(naleznosci)) };
    przesylkiApiStub = { szukaj: vi.fn().mockReturnValue(of(przesylki)) };
    obywateleApiStub = { getById: vi.fn().mockReturnValue(of(obywatel1)) };

    TestBed.configureTestingModule({
      imports: [PulpitStrona],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authStub },
        { provide: SprawyApi, useValue: sprawyApiStub },
        { provide: PlatnosciApi, useValue: platnosciApiStub },
        { provide: PrzesylkiApi, useValue: przesylkiApiStub },
        { provide: ObywateleApi, useValue: obywateleApiStub },
      ],
    });
  });

  it('wczytuje sprawy/należności/przesyłki i wylicza liczniki kafelków', () => {
    const fixture = TestBed.createComponent(PulpitStrona);
    fixture.detectChanges();

    expect(sprawyApiStub.szukaj).toHaveBeenCalledWith({ rozmiarStrony: 200 });
    expect(fixture.componentInstance['wczytywanie']()).toBe(false);

    // moje sprawy: tylko referent Zielińska, nie zamknięte => spr-p3, spr-p1, spr-p2, spr-inny-wydzial
    expect(fixture.componentInstance['liczbaSprawWToku']()).toBe(4);

    // terminy krytyczne: spr-p1 (przeterminowany), spr-p2 (4 dni) - spr-inny-wydzial ma brak terminu
    expect(fixture.componentInstance['liczbaTerminowKrytycznych']()).toBe(2);

    // przybliżony wydział = wydział najnowszej sprawy (spr-p3, sort desc po dataWszczecia)
    expect(fixture.componentInstance['przyblizonyWydzial']()).toBe('Wydział Ochrony Środowiska');

    // nierozliczone należności wydziału Ochrony Środowiska: tylko n-1 (n-2 rozliczona, n-3 inny wydział)
    expect(fixture.componentInstance['liczbaNierozliczonychNaleznosci']()).toBe(1);

    // przesyłki oczekujące wydziału Ochrony Środowiska: tylko p-1 (p-2 doreczone, p-3 inny wydział)
    expect(fixture.componentInstance['liczbaPrzesylekOczekujacych']()).toBe(1);
  });

  it('dociąga nazwę strony dla spraw referenta przez ObywateleApi.getById', () => {
    const fixture = TestBed.createComponent(PulpitStrona);
    fixture.detectChanges();

    expect(obywateleApiStub.getById).toHaveBeenCalledWith('ob-1');
    expect(fixture.componentInstance['nazwaObywatela']('ob-1')).toBe('Kowalski Jan');
  });

  it('nie wywołuje API dla użytkownika bez żadnej sprawy - przybliżony wydział jest pusty', () => {
    sprawyApiStub.szukaj.mockReturnValue(
      of({ dane: [], strona: 0, rozmiarStrony: 200, liczbaWszystkich: 0 }),
    );

    const fixture = TestBed.createComponent(PulpitStrona);
    fixture.detectChanges();

    expect(fixture.componentInstance['przyblizonyWydzial']()).toBe('');
    expect(fixture.componentInstance['liczbaNierozliczonychNaleznosci']()).toBe(0);
    expect(fixture.componentInstance['liczbaPrzesylekOczekujacych']()).toBe(0);
  });

  it('pokazuje komunikat błędu, gdy wczytanie spraw zawiedzie, bez crasha pozostałych kafelków', () => {
    sprawyApiStub.szukaj.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = TestBed.createComponent(PulpitStrona);
    fixture.detectChanges();

    expect(fixture.componentInstance['komunikatBledu']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
    expect(fixture.componentInstance['liczbaSprawWToku']()).toBe(0);
    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
  });

  it('pokazuje komunikat błędu, gdy wczytanie należności zawiedzie', () => {
    platnosciApiStub.naleznosci.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );

    const fixture = TestBed.createComponent(PulpitStrona);
    fixture.detectChanges();

    expect(fixture.componentInstance['komunikatBledu']()).toBe('Nie znaleziono żądanego zasobu.');
    expect(fixture.componentInstance['liczbaNierozliczonychNaleznosci']()).toBe(0);
  });
});
