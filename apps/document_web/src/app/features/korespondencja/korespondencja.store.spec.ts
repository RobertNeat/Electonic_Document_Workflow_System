import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import type { Przesylka } from '@electronic-document-workflow/document-shared-dto';
import { KorespondencjaStore } from './korespondencja.store';
import { PrzesylkiApi } from '../../core/api/przesylki.api';

describe('KorespondencjaStore', () => {
  let apiStub: { szukaj: ReturnType<typeof vi.fn>; nadaj: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    apiStub = { szukaj: vi.fn(), nadaj: vi.fn() };
    TestBed.configureTestingModule({
      providers: [{ provide: PrzesylkiApi, useValue: apiStub }],
    });
  });

  it('szukaj() wypełnia przesylki() z odpowiedzi API', async () => {
    const wynik: Przesylka[] = [{ id: 'p-1' } as Przesylka];
    apiStub.szukaj.mockReturnValue(of(wynik));

    const store = TestBed.inject(KorespondencjaStore);
    await store.szukaj({ kierunek: 'wychodzaca' });

    expect(apiStub.szukaj).toHaveBeenCalledWith({ kierunek: 'wychodzaca' });
    expect(store.przesylki()).toEqual(wynik);
    expect(store.loading()).toBe(false);
    expect(store.blad()).toBeNull();
  });

  it('szukaj() przy błędzie HTTP ustawia czytelny komunikat w blad()', async () => {
    apiStub.szukaj.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const store = TestBed.inject(KorespondencjaStore);
    await store.szukaj();

    expect(store.blad()).toBe('Brak połączenia z serwerem (sieć lub CORS).');
    expect(store.loading()).toBe(false);
  });

  it('nadaj() zwraca utworzoną przesyłkę przy sukcesie', async () => {
    const przesylka = { id: 'p-nowa', numerReferencyjny: 'ADE/2026/00001' } as Przesylka;
    apiStub.nadaj.mockReturnValue(of(przesylka));

    const store = TestBed.inject(KorespondencjaStore);
    const wynik = await store.nadaj({
      sprawaId: 's-1',
      dokumentId: 'd-1',
      obywatelId: 'ob-1',
      kanal: 'e-doreczenia',
    });

    expect(wynik).toEqual(przesylka);
  });

  it('nadaj() zwraca komunikat błędu (string) przy niepowodzeniu, bez pisania do blad()', () => {
    apiStub.nadaj.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { komunikat: 'Dokument nie jest podpisaną aktualną wersją' },
          }),
      ),
    );

    const store = TestBed.inject(KorespondencjaStore);
    return store
      .nadaj({ sprawaId: 's-1', dokumentId: 'd-1', obywatelId: 'ob-1', kanal: 'email' })
      .then((wynik) => {
        expect(wynik).toBe('Dokument nie jest podpisaną aktualną wersją');
        expect(store.blad()).toBeNull();
      });
  });
});
