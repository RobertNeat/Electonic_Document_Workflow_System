import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import type {
  Sprawa,
  StronaWynikow,
  Obywatel,
  DokumentListElement,
  Przesylka,
} from '@electronic-document-workflow/document-shared-dto';
import { KorespondencjaWyslij } from './korespondencja-wyslij';
import { KorespondencjaStore } from '../korespondencja.store';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { ObywateleApi } from '../../../core/api/obywatele.api';
import { DokumentyApi } from '../../../core/api/dokumenty.api';

describe('KorespondencjaWyslij', () => {
  let storeStub: { nadaj: ReturnType<typeof vi.fn> };
  let sprawyApiStub: { szukaj: ReturnType<typeof vi.fn> };
  let obywateleApiStub: { szukaj: ReturnType<typeof vi.fn> };
  let dokumentyApiStub: { listaDlaSprawy: ReturnType<typeof vi.fn> };

  const sprawy: Sprawa[] = [
    { id: 's-1', znakSprawy: 'OR.1.1.2026', tytul: 'Sprawa 1', obywatelId: 'ob-1' } as Sprawa,
  ];
  const obywatele: Obywatel[] = [
    {
      id: 'ob-1',
      typ: 'osoba_fizyczna',
      imie: 'Jan',
      nazwisko: 'Kowalski',
      email: 'jan@example.com',
    } as Obywatel,
  ];

  function strona(dane: Sprawa[]): StronaWynikow<Sprawa> {
    return { dane, strona: 0, rozmiarStrony: 200, liczbaWszystkich: dane.length };
  }

  beforeEach(() => {
    storeStub = { nadaj: vi.fn() };
    sprawyApiStub = { szukaj: vi.fn().mockReturnValue(of(strona(sprawy))) };
    obywateleApiStub = { szukaj: vi.fn().mockReturnValue(of(obywatele)) };
    dokumentyApiStub = { listaDlaSprawy: vi.fn().mockReturnValue(of([])) };

    TestBed.configureTestingModule({
      imports: [KorespondencjaWyslij],
      providers: [
        provideRouter([]),
        { provide: KorespondencjaStore, useValue: storeStub },
        { provide: SprawyApi, useValue: sprawyApiStub },
        { provide: ObywateleApi, useValue: obywateleApiStub },
        { provide: DokumentyApi, useValue: dokumentyApiStub },
      ],
    });
  });

  it('wczytuje listę spraw z rozmiarStrony=200 i listę obywateli przy starcie', async () => {
    const fixture = TestBed.createComponent(KorespondencjaWyslij);
    fixture.detectChanges();
    await Promise.resolve();

    expect(sprawyApiStub.szukaj).toHaveBeenCalledWith({ rozmiarStrony: 200 });
    expect(obywateleApiStub.szukaj).toHaveBeenCalled();
    expect(fixture.componentInstance['opcjeSpraw']()).toEqual([
      { etykieta: 'OR.1.1.2026 — Sprawa 1', wartosc: 's-1' },
    ]);
  });

  it('wybranie sprawy dociąga dokumenty filtrowane po statusPodpisuAktualnej === poprawny', async () => {
    const dokumenty: DokumentListElement[] = [
      {
        id: 'd-1',
        sprawaId: 's-1',
        typDokumentuId: 't-1',
        nazwa: 'Decyzja podpisana',
        aktualnaWersja: 2,
        dataUtworzenia: '2026-10-01',
        znakSprawy: 'OR.1.1.2026',
        statusPodpisuAktualnej: 'poprawny',
      },
      {
        id: 'd-2',
        sprawaId: 's-1',
        typDokumentuId: 't-1',
        nazwa: 'Decyzja niepodpisana',
        aktualnaWersja: 1,
        dataUtworzenia: '2026-10-01',
        znakSprawy: 'OR.1.1.2026',
        statusPodpisuAktualnej: 'brak',
      },
    ];
    dokumentyApiStub.listaDlaSprawy.mockReturnValue(of(dokumenty));

    const fixture = TestBed.createComponent(KorespondencjaWyslij);
    fixture.detectChanges();
    await Promise.resolve();

    fixture.componentInstance.wybierzSprawe('s-1');
    await Promise.resolve();

    expect(dokumentyApiStub.listaDlaSprawy).toHaveBeenCalledWith('s-1');
    expect(fixture.componentInstance['opcjeDokumentow']()).toEqual([
      { etykieta: 'Decyzja podpisana', wartosc: 'd-1' },
    ]);
    expect(fixture.componentInstance['obywatelId']()).toBe('ob-1');
  });

  it('zmiana sprawy czyści poprzednio wybrany dokument i listę dokumentów', async () => {
    dokumentyApiStub.listaDlaSprawy.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(KorespondencjaWyslij);
    fixture.detectChanges();
    await Promise.resolve();

    fixture.componentInstance['dokumentId'].set('d-stary');
    fixture.componentInstance.wybierzSprawe('s-1');

    expect(fixture.componentInstance['dokumentId']()).toBeNull();
    expect(fixture.componentInstance['opcjeDokumentow']()).toEqual([]);
  });

  it('wyslij() surowy błąd walidacji z backendu (np. dokument niepodpisany) pokazuje się w blad()', async () => {
    storeStub.nadaj.mockResolvedValue('Aktualna wersja dokumentu nie jest podpisana');

    const fixture = TestBed.createComponent(KorespondencjaWyslij);
    fixture.detectChanges();
    await Promise.resolve();

    fixture.componentInstance['sprawaId'].set('s-1');
    fixture.componentInstance['dokumentId'].set('d-1');
    fixture.componentInstance['obywatelId'].set('ob-1');
    fixture.componentInstance.wyslij();
    await Promise.resolve();

    expect(fixture.componentInstance['blad']()).toBe(
      'Aktualna wersja dokumentu nie jest podpisana',
    );
    expect(fixture.componentInstance['potwierdzenie']()).toBeNull();
  });

  it('wyslij() przy sukcesie ustawia potwierdzenie()', async () => {
    const przesylka = { id: 'p-nowa', numerReferencyjny: 'MAIL/2026/00001' } as Przesylka;
    storeStub.nadaj.mockResolvedValue(przesylka);

    const fixture = TestBed.createComponent(KorespondencjaWyslij);
    fixture.detectChanges();
    await Promise.resolve();

    fixture.componentInstance['sprawaId'].set('s-1');
    fixture.componentInstance['dokumentId'].set('d-1');
    fixture.componentInstance['obywatelId'].set('ob-1');
    fixture.componentInstance.wyslij();
    await Promise.resolve();

    expect(fixture.componentInstance['potwierdzenie']()).toEqual(przesylka);
  });

  it('wyslij() bez wybranej sprawy/dokumentu/adresata pokazuje komunikat walidacji lokalnej', () => {
    const fixture = TestBed.createComponent(KorespondencjaWyslij);
    fixture.detectChanges();

    fixture.componentInstance.wyslij();

    expect(fixture.componentInstance['blad']()).toBe(
      'Wybierz sprawę, podpisany dokument oraz adresata.',
    );
    expect(storeStub.nadaj).not.toHaveBeenCalled();
  });

  it('błąd wczytywania listy spraw jest widoczny w bladListSprawy()', async () => {
    sprawyApiStub.szukaj.mockReturnValue(throwError(() => new Error('siec')));

    const fixture = TestBed.createComponent(KorespondencjaWyslij);
    fixture.detectChanges();
    await Promise.resolve();

    expect(fixture.componentInstance['bladListSprawy']()).toBe(
      'Nie udało się wczytać listy spraw.',
    );
  });
});
