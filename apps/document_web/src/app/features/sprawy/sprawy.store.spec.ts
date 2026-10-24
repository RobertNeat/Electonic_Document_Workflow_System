import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import type { Sprawa, StronaWynikow } from '@electronic-document-workflow/document-shared-dto';
import { SprawyStore, komunikatBleduHttp } from './sprawy.store';
import { SprawyApi } from '../../core/api/sprawy.api';

describe('komunikatBleduHttp', () => {
  it('mapuje 403 na komunikat o braku uprawnień', () => {
    const err = new HttpErrorResponse({ status: 403 });
    expect(komunikatBleduHttp(err, 'domyślny')).toBe('Brak uprawnień do wykonania tej operacji.');
  });

  it('mapuje 404 na komunikat "nie znaleziono"', () => {
    const err = new HttpErrorResponse({ status: 404 });
    expect(komunikatBleduHttp(err, 'domyślny')).toBe('Nie znaleziono żądanego zasobu.');
  });

  it('mapuje status 0 (sieć/CORS) na komunikat o braku połączenia', () => {
    const err = new HttpErrorResponse({ status: 0 });
    expect(komunikatBleduHttp(err, 'domyślny')).toBe('Brak połączenia z serwerem (sieć lub CORS).');
  });

  it('używa komunikatu z ciała odpowiedzi backendu, gdy jest dostępny', () => {
    const err = new HttpErrorResponse({
      status: 409,
      error: { komunikat: 'Niedozwolone przejście' },
    });
    expect(komunikatBleduHttp(err, 'domyślny')).toBe('Niedozwolone przejście');
  });

  it('zwraca domyślny komunikat dla błędu, który nie jest HttpErrorResponse', () => {
    expect(komunikatBleduHttp(new Error('coś innego'), 'domyślny')).toBe('domyślny');
  });
});

describe('SprawyStore', () => {
  let apiStub: {
    szukaj: ReturnType<typeof vi.fn>;
    rejestrujSprawe: ReturnType<typeof vi.fn>;
    definicjeObiegow: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    apiStub = {
      szukaj: vi.fn(),
      rejestrujSprawe: vi.fn(),
      definicjeObiegow: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [{ provide: SprawyApi, useValue: apiStub }],
    });
  });

  it('szukaj() wypełnia sprawy/strona/rozmiarStrony/liczbaWszystkich z odpowiedzi', async () => {
    const strona: StronaWynikow<Sprawa> = {
      dane: [{ id: 'spr-1' } as Sprawa],
      strona: 2,
      rozmiarStrony: 10,
      liczbaWszystkich: 25,
    };
    apiStub.szukaj.mockReturnValue(of(strona));

    const store = TestBed.inject(SprawyStore);
    await store.szukaj({ strona: 2, rozmiarStrony: 10 });

    expect(store.sprawy()).toEqual(strona.dane);
    expect(store.strona()).toBe(2);
    expect(store.rozmiarStrony()).toBe(10);
    expect(store.liczbaWszystkich()).toBe(25);
    expect(store.loading()).toBe(false);
    expect(store.blad()).toBeNull();
  });

  it('szukaj() przy błędzie 403 ustawia czytelny komunikat w blad()', async () => {
    apiStub.szukaj.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));

    const store = TestBed.inject(SprawyStore);
    await store.szukaj();

    expect(store.blad()).toBe('Brak uprawnień do wykonania tej operacji.');
    expect(store.loading()).toBe(false);
  });

  it('dodajSprawe() zwraca komunikat błędu (string) przy niepowodzeniu, bez pisania do blad()', async () => {
    apiStub.rejestrujSprawe.mockReturnValue(
      throwError(
        () => new HttpErrorResponse({ status: 400, error: { komunikat: 'Nieznany wydział' } }),
      ),
    );

    const store = TestBed.inject(SprawyStore);
    const wynik = await store.dodajSprawe({
      definicjaObieguId: 'obieg-decyzja',
      jrwa: '6220',
      kategoriaArchiwalna: 'B10',
      tytul: 'T',
      obywatelId: 'ob-1',
      wydzial: 'X',
      referent: 'Jan',
    });

    expect(wynik).toBe('Nieznany wydział');
    expect(store.blad()).toBeNull();
  });

  it('dodajSprawe() zwraca utworzoną sprawę przy sukcesie', async () => {
    const sprawa = { id: 'spr-9', znakSprawy: 'OR.1510.1.2026' } as Sprawa;
    apiStub.rejestrujSprawe.mockReturnValue(of(sprawa));

    const store = TestBed.inject(SprawyStore);
    const wynik = await store.dodajSprawe({
      definicjaObieguId: 'obieg-decyzja',
      jrwa: '1510',
      kategoriaArchiwalna: 'B10',
      tytul: 'T',
      obywatelId: 'ob-1',
      wydzial: 'Wydział Organizacyjny',
      referent: 'Jan',
    });

    expect(wynik).toEqual(sprawa);
  });
});
