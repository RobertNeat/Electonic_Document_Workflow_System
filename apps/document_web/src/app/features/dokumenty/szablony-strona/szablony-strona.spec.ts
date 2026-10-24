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

  it('wgraj() dopisuje lokalnie nowy wpis szablonu z rozszerzeniem .jrxml', () => {
    const fixture = TestBed.createComponent(SzablonyStrona);
    fixture.detectChanges();

    const plik = { name: 'nowy_szablon.jrxml' } as File;
    fixture.componentInstance['wgraj']({ files: [plik] } as never);

    expect(fixture.componentInstance['szablony']().length).toBe(2);
    expect(fixture.componentInstance['szablony']()[0].nazwa).toBe('nowy_szablon.jrxml');
    expect(fixture.componentInstance['uploadWidoczny']()).toBe(false);
  });
});
