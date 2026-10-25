import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { NEVER, of, throwError } from 'rxjs';
import type {
  SzablonJasper,
  TypDokumentu,
} from '@electronic-document-workflow/document-shared-dto';
import { SzablonyStrona } from './szablony-strona';
import { DokumentyApi } from '../../../core/api/dokumenty.api';
import { SprawyApi } from '../../../core/api/sprawy.api';

describe('SzablonyStrona', () => {
  let dokumentyApiStub: {
    szablony: ReturnType<typeof vi.fn>;
    historiaSzablonu: ReturnType<typeof vi.fn>;
    wgrajSzablon: ReturnType<typeof vi.fn>;
  };
  let sprawyApiStub: { typyDokumentow: ReturnType<typeof vi.fn> };

  const typy: TypDokumentu[] = [{ id: 'td-3', nazwa: 'Decyzja administracyjna', opis: '' }];

  const szablony: SzablonJasper[] = [
    {
      id: 'sz-1',
      typDokumentuId: 'td-3',
      nazwa: 'decyzja_srodowiskowa.jrxml',
      wersja: 3,
      dataWgrania: '2026-08-15',
      autor: 'Administrator',
    },
  ];

  beforeEach(() => {
    dokumentyApiStub = {
      szablony: vi.fn().mockReturnValue(of(szablony)),
      historiaSzablonu: vi.fn().mockReturnValue(of(szablony)),
      wgrajSzablon: vi.fn(),
    };
    sprawyApiStub = { typyDokumentow: vi.fn().mockReturnValue(of(typy)) };

    TestBed.configureTestingModule({
      imports: [SzablonyStrona],
      providers: [
        { provide: DokumentyApi, useValue: dokumentyApiStub },
        { provide: SprawyApi, useValue: sprawyApiStub },
      ],
    });
  });

  it('wczytuje szablony i typy dokumentów przy montowaniu', () => {
    const fixture = TestBed.createComponent(SzablonyStrona);
    fixture.detectChanges();

    expect(dokumentyApiStub.szablony).toHaveBeenCalled();
    expect(sprawyApiStub.typyDokumentow).toHaveBeenCalled();
    expect(fixture.componentInstance['szablony']()).toEqual(szablony);
    expect(fixture.componentInstance['nazwaTypu']('td-3')).toBe('Decyzja administracyjna');
    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
  });

  it('pokazuje stan "wczytywanie" odrębny od pustej listy, dopóki żądanie nie wróci', () => {
    dokumentyApiStub.szablony.mockReturnValue(NEVER);

    const fixture = TestBed.createComponent(SzablonyStrona);
    fixture.detectChanges();

    expect(fixture.componentInstance['wczytywanie']()).toBe(true);
    expect(fixture.componentInstance['szablony']()).toEqual([]);
  });

  it('pokazuje komunikat błędu, gdy szablony() zawiedzie', () => {
    dokumentyApiStub.szablony.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0 })),
    );

    const fixture = TestBed.createComponent(SzablonyStrona);
    fixture.detectChanges();

    expect(fixture.componentInstance['komunikatBledu']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
  });

  it('pokazHistorie() woła historiaSzablonu(id) i wypełnia historiaWybranego()', () => {
    const fixture = TestBed.createComponent(SzablonyStrona);
    fixture.detectChanges();

    fixture.componentInstance['pokazHistorie'](szablony[0]);

    expect(dokumentyApiStub.historiaSzablonu).toHaveBeenCalledWith('sz-1');
    expect(fixture.componentInstance['historiaWybranego']()).toEqual(szablony);
    expect(fixture.componentInstance['historiaWidoczna']()).toBe(true);
    expect(fixture.componentInstance['historiaWczytywanie']()).toBe(false);
  });

  it('pokazHistorie() pokazuje komunikat błędu, gdy historiaSzablonu() zawiedzie', () => {
    dokumentyApiStub.historiaSzablonu.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );

    const fixture = TestBed.createComponent(SzablonyStrona);
    fixture.detectChanges();

    fixture.componentInstance['pokazHistorie'](szablony[0]);

    expect(fixture.componentInstance['historiaBlad']()).toBe('Nie znaleziono żądanego zasobu.');
    expect(fixture.componentInstance['historiaWybranego']()).toEqual([]);
  });

  it('wgraj() woła wgrajSzablon() z typem dokumentu i dopisuje wynik z API do listy', () => {
    const nowySzablon: SzablonJasper = {
      id: 'sz-nowy',
      typDokumentuId: 'td-3',
      nazwa: 'nowy_szablon.jrxml',
      wersja: 1,
      dataWgrania: '2026-10-25',
      autor: 'Administrator',
    };
    dokumentyApiStub.wgrajSzablon.mockReturnValue(of(nowySzablon));

    const fixture = TestBed.createComponent(SzablonyStrona);
    fixture.detectChanges();
    fixture.componentInstance['otworzUpload']();

    const plik = { name: 'nowy_szablon.jrxml' } as File;
    fixture.componentInstance['wgraj']({ files: [plik] } as never);

    expect(dokumentyApiStub.wgrajSzablon).toHaveBeenCalledWith('td-3', 'nowy_szablon.jrxml', plik);
    expect(fixture.componentInstance['szablony']().length).toBe(2);
    expect(fixture.componentInstance['szablony']()[0]).toEqual(nowySzablon);
    expect(fixture.componentInstance['uploadWidoczny']()).toBe(false);
  });

  it('wgraj() pokazuje komunikat błędu i nie zamyka dialogu, gdy wgrajSzablon() zawiedzie', () => {
    dokumentyApiStub.wgrajSzablon.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 400 })),
    );

    const fixture = TestBed.createComponent(SzablonyStrona);
    fixture.detectChanges();
    fixture.componentInstance['otworzUpload']();

    const plik = { name: 'nowy_szablon.jrxml' } as File;
    fixture.componentInstance['wgraj']({ files: [plik] } as never);

    expect(fixture.componentInstance['uploadBlad']()).toBeTruthy();
    expect(fixture.componentInstance['uploadWidoczny']()).toBe(true);
    expect(fixture.componentInstance['szablony']().length).toBe(1);
  });

  it('wgraj() odrzuca plik bez rozszerzenia .jrxml bez wywołania API', () => {
    const fixture = TestBed.createComponent(SzablonyStrona);
    fixture.detectChanges();
    fixture.componentInstance['otworzUpload']();

    const plik = { name: 'nie_jrxml.txt' } as File;
    fixture.componentInstance['wgraj']({ files: [plik] } as never);

    expect(dokumentyApiStub.wgrajSzablon).not.toHaveBeenCalled();
    expect(fixture.componentInstance['uploadBlad']()).toBeTruthy();
  });
});
