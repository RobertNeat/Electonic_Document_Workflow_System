import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { NEVER, of, throwError } from 'rxjs';
import type {
  Dokument,
  Sprawa,
  TypDokumentu,
  WersjaDokumentu,
} from '@electronic-document-workflow/document-shared-dto';
import { DokumentSzczegoly } from './dokument-szczegoly';
import { DokumentyApi } from '../../../core/api/dokumenty.api';
import { SprawyApi } from '../../../core/api/sprawy.api';

describe('DokumentSzczegoly', () => {
  let dokumentyApiStub: {
    getById: ReturnType<typeof vi.fn>;
    wersje: ReturnType<typeof vi.fn>;
  };
  let sprawyApiStub: {
    getById: ReturnType<typeof vi.fn>;
    typyDokumentow: ReturnType<typeof vi.fn>;
  };

  const dokument: Dokument = {
    id: 'd-2',
    sprawaId: 's-1',
    typDokumentuId: 'td-3',
    nazwa: 'Decyzja środowiskowa',
    aktualnaWersja: 2,
    dataUtworzenia: '2026-10-05',
  };

  const wersje: WersjaDokumentu[] = [
    {
      id: 'wd-1',
      dokumentId: 'd-2',
      wersja: 1,
      podpisana: false,
      dataUtworzenia: '2026-09-28',
      autor: 'Katarzyna Zielińska',
      statusPodpisu: 'brak',
    },
    {
      id: 'wd-2',
      dokumentId: 'd-2',
      wersja: 2,
      podpisana: true,
      dataUtworzenia: '2026-10-05',
      autor: 'Katarzyna Zielińska',
      statusPodpisu: 'poprawny',
    },
  ];

  const typy: TypDokumentu[] = [{ id: 'td-3', nazwa: 'Decyzja administracyjna', opis: '' }];

  beforeEach(() => {
    dokumentyApiStub = {
      getById: vi.fn().mockReturnValue(of(dokument)),
      wersje: vi.fn().mockReturnValue(of(wersje)),
    };
    sprawyApiStub = {
      getById: vi.fn().mockReturnValue(of({ id: 's-1', znakSprawy: 'OR.1.1.2026' } as Sprawa)),
      typyDokumentow: vi.fn().mockReturnValue(of(typy)),
    };

    TestBed.configureTestingModule({
      imports: [DokumentSzczegoly],
      providers: [
        provideRouter([]),
        { provide: DokumentyApi, useValue: dokumentyApiStub },
        { provide: SprawyApi, useValue: sprawyApiStub },
      ],
    });
  });

  async function wczytaj() {
    const fixture = TestBed.createComponent(DokumentSzczegoly);
    fixture.componentRef.setInput('id', 'd-2');
    fixture.detectChanges();
    await Promise.resolve();
    await Promise.resolve();
    fixture.detectChanges();
    return fixture;
  }

  it('wczytuje dokument, wersje (sortowane malejąco), sprawę i typ dokumentu', async () => {
    const fixture = await wczytaj();

    expect(fixture.componentInstance['status']()).toBe('ok');
    expect(fixture.componentInstance['dokument']()).toEqual(dokument);
    expect(fixture.componentInstance['wersje']().map((w: WersjaDokumentu) => w.wersja)).toEqual([
      2, 1,
    ]);
    expect(fixture.componentInstance['sprawa']()?.znakSprawy).toBe('OR.1.1.2026');
    expect(fixture.componentInstance['typDokumentu']()?.nazwa).toBe('Decyzja administracyjna');
    expect(dokumentyApiStub.wersje).toHaveBeenCalledWith('d-2');
    expect(sprawyApiStub.getById).toHaveBeenCalledWith('s-1');
  });

  it('pokazuje stan "ladowanie" dopóki getById() nie wróci', () => {
    dokumentyApiStub.getById.mockReturnValue(NEVER);

    const fixture = TestBed.createComponent(DokumentSzczegoly);
    fixture.componentRef.setInput('id', 'd-2');
    fixture.detectChanges();

    expect(fixture.componentInstance['status']()).toBe('ladowanie');
    expect(fixture.nativeElement.querySelector('#dokument-szczegoly-wczytywanie')).toBeTruthy();
  });

  it('404 na getById ustawia stan "nieznaleziony"', async () => {
    dokumentyApiStub.getById.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );

    const fixture = await wczytaj();

    expect(fixture.componentInstance['status']()).toBe('nieznaleziony');
  });

  it('błąd sieci na getById ustawia stan "blad" z komunikatem', async () => {
    dokumentyApiStub.getById.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0 })),
    );

    const fixture = await wczytaj();

    expect(fixture.componentInstance['status']()).toBe('blad');
    expect(fixture.componentInstance['komunikatBledu']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
  });

  it('błąd wczytywania sprawy nie blokuje stanu "ok" - sprawa zostaje undefined', async () => {
    sprawyApiStub.getById.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = await wczytaj();

    expect(fixture.componentInstance['status']()).toBe('ok');
    expect(fixture.componentInstance['sprawa']()).toBeUndefined();
  });

  it('błąd wczytywania wersji przełącza stan na "blad" (timeline jest głównym elementem strony)', async () => {
    dokumentyApiStub.wersje.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = await wczytaj();

    expect(fixture.componentInstance['status']()).toBe('blad');
  });

  it('podpisz() mutuje lokalnie sygnał wersji (brak podłączonego realnego endpointu)', async () => {
    const fixture = await wczytaj();

    fixture.componentInstance['podpisz'](wersje[0]);

    const zaktualizowana = fixture.componentInstance['wersje']().find(
      (w: WersjaDokumentu) => w.id === 'wd-1',
    );
    expect(zaktualizowana?.podpisana).toBe(true);
    expect(zaktualizowana?.statusPodpisu).toBe('poprawny');
  });
});
