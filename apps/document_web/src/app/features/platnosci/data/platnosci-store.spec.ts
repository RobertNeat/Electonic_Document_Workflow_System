import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import type {
  NaleznoscListElement,
  WynikImportuCamt053,
  Wplata,
} from '@electronic-document-workflow/document-shared-dto';
import { PlatnosciStore } from './platnosci-store';
import { PlatnosciApi } from '../../../core/api/platnosci.api';

describe('PlatnosciStore', () => {
  let apiStub: {
    naleznosci: ReturnType<typeof vi.fn>;
    wplaty: ReturnType<typeof vi.fn>;
    dopasujWplate: ReturnType<typeof vi.fn>;
    importujCamt053: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    apiStub = {
      naleznosci: vi.fn(),
      wplaty: vi.fn(),
      dopasujWplate: vi.fn(),
      importujCamt053: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [{ provide: PlatnosciApi, useValue: apiStub }],
    });
  });

  it('szukajNaleznosci() wypełnia naleznosci() z odpowiedzi API', async () => {
    const wynik: NaleznoscListElement[] = [{ id: 'n-1' } as NaleznoscListElement];
    apiStub.naleznosci.mockReturnValue(of(wynik));

    const store = TestBed.inject(PlatnosciStore);
    await store.szukajNaleznosci({ status: 'nierozliczona' });

    expect(apiStub.naleznosci).toHaveBeenCalledWith({ status: 'nierozliczona' });
    expect(store.naleznosci()).toEqual(wynik);
    expect(store.loading()).toBe(false);
    expect(store.blad()).toBeNull();
  });

  it('szukajNaleznosci() przy błędzie HTTP ustawia czytelny komunikat w blad()', async () => {
    apiStub.naleznosci.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const store = TestBed.inject(PlatnosciStore);
    await store.szukajNaleznosci();

    expect(store.blad()).toBe('Brak połączenia z serwerem (sieć lub CORS).');
    expect(store.loading()).toBe(false);
  });

  it('szukajWplat() wypełnia wplaty() z odpowiedzi API', async () => {
    const wynik: Wplata[] = [{ id: 'w-1' } as Wplata];
    apiStub.wplaty.mockReturnValue(of(wynik));

    const store = TestBed.inject(PlatnosciStore);
    await store.szukajWplat({});

    expect(store.wplaty()).toEqual(wynik);
  });

  it('dopasujWplate() zwraca zaktualizowaną wpłatę przy sukcesie', async () => {
    const wplata = { id: 'w-1', naleznoscId: 'n-1' } as Wplata;
    apiStub.dopasujWplate.mockReturnValue(of(wplata));

    const store = TestBed.inject(PlatnosciStore);
    const wynik = await store.dopasujWplate('w-1', 'n-1');

    expect(apiStub.dopasujWplate).toHaveBeenCalledWith('w-1', 'n-1');
    expect(wynik).toEqual(wplata);
  });

  it('dopasujWplate() zwraca komunikat błędu (string) przy niepowodzeniu, bez pisania do blad()', async () => {
    apiStub.dopasujWplate.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));

    const store = TestBed.inject(PlatnosciStore);
    const wynik = await store.dopasujWplate('w-1', 'n-1');

    expect(wynik).toBe('Brak uprawnień do wykonania tej operacji.');
    expect(store.blad()).toBeNull();
  });

  it('importujWyciag() zwraca wynik importu wraz z nazwą pliku przy sukcesie', async () => {
    const wynikApi: WynikImportuCamt053 = {
      liczbaTransakcji: 3,
      liczbaDopasowanychAutomatycznie: 2,
      liczbaDoRekoncyliacji: 1,
      utworzoneWplaty: [],
    };
    apiStub.importujCamt053.mockReturnValue(of(wynikApi));
    const plik = new File(['<Document></Document>'], 'wyciag.xml');

    const store = TestBed.inject(PlatnosciStore);
    const wynik = await store.importujWyciag(plik);

    expect(apiStub.importujCamt053).toHaveBeenCalledWith(plik);
    expect(wynik).toEqual({ nazwaPliku: 'wyciag.xml', wynik: wynikApi });
  });

  it('importujWyciag() zwraca komunikat błędu (string) przy niepowodzeniu', async () => {
    apiStub.importujCamt053.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({ status: 400, error: { komunikat: 'Plik nie jest plikiem XML' } }),
      ),
    );
    const plik = new File(['nie-xml'], 'wyciag.txt');

    const store = TestBed.inject(PlatnosciStore);
    const wynik = await store.importujWyciag(plik);

    expect(wynik).toBe('Plik nie jest plikiem XML');
  });
});
