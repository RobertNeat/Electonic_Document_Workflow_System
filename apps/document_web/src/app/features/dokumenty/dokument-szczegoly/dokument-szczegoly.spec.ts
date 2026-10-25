import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { NEVER, of, throwError } from 'rxjs';
import type {
  Certyfikat,
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
    listaCertyfikatow: ReturnType<typeof vi.fn>;
    podpisz: ReturnType<typeof vi.fn>;
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

  // wersja 1: historyczna, już podpisana. wersja 2: AKTUALNA (dokument.aktualnaWersja === 2),
  // niepodpisana - tylko aktualna wersja może być podpisana przez backend (podpiszDokument
  // zawsze operuje na aktualnaWersja), stąd fixture musi to odzwierciedlać.
  const wersje: WersjaDokumentu[] = [
    {
      id: 'wd-1',
      dokumentId: 'd-2',
      wersja: 1,
      podpisana: true,
      dataUtworzenia: '2026-09-28',
      autor: 'Katarzyna Zielińska',
      statusPodpisu: 'poprawny',
    },
    {
      id: 'wd-2',
      dokumentId: 'd-2',
      wersja: 2,
      podpisana: false,
      dataUtworzenia: '2026-10-05',
      autor: 'Katarzyna Zielińska',
      statusPodpisu: 'brak',
    },
  ];

  const typy: TypDokumentu[] = [{ id: 'td-3', nazwa: 'Decyzja administracyjna', opis: '' }];

  const certyfikaty: Certyfikat[] = [
    {
      id: 'cert-1',
      podmiot: 'CN=Katarzyna Zielińska, O=Urząd, C=PL',
      numerSeryjny: '7A3F1B2C9D0E4455',
      dataWydania: '2026-01-10',
      dataWygasniecia: '2027-01-10',
      status: 'aktywny',
      wydawca: 'Demo CA Issuing',
    },
    {
      id: 'cert-2',
      podmiot: 'CN=Jan Kowalski, O=Urząd, C=PL',
      numerSeryjny: '1122334455667788',
      dataWydania: '2020-01-10',
      dataWygasniecia: '2021-01-10',
      status: 'wygasly',
      wydawca: 'Demo CA Issuing',
    },
  ];

  beforeEach(() => {
    dokumentyApiStub = {
      getById: vi.fn().mockReturnValue(of(dokument)),
      wersje: vi.fn().mockReturnValue(of(wersje)),
      listaCertyfikatow: vi.fn().mockReturnValue(of(certyfikaty)),
      podpisz: vi.fn(),
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

  it('podpisz() otwiera cert-picker i wczytuje listę certyfikatów', async () => {
    const fixture = await wczytaj();

    fixture.componentInstance['podpisz'](wersje[1]);

    expect(fixture.componentInstance['certPodpisWidoczny']()).toBe(true);
    expect(fixture.componentInstance['certyfikaty']()).toEqual(certyfikaty);
    expect(dokumentyApiStub.listaCertyfikatow).toHaveBeenCalled();
  });

  it('przycisk "Podpisz" pokazuje się tylko dla aktualnej (niepodpisanej) wersji', async () => {
    const fixture = await wczytaj();

    const html = fixture.nativeElement.innerHTML as string;
    expect(html).toContain('Podpisz');
    expect(html).toContain('Szczegóły podpisu');
  });

  it('certyfikatAktywny() odróżnia certyfikat aktywny od wygasłego', async () => {
    const fixture = await wczytaj();

    expect(fixture.componentInstance['certyfikatAktywny'](certyfikaty[0])).toBe(true);
    expect(fixture.componentInstance['certyfikatAktywny'](certyfikaty[1])).toBe(false);
  });

  it('błąd wczytywania listy certyfikatów ustawia komunikat błędu cert-pickera', async () => {
    dokumentyApiStub.listaCertyfikatow.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0 })),
    );
    const fixture = await wczytaj();

    fixture.componentInstance['podpisz'](wersje[1]);

    expect(fixture.componentInstance['certBladListy']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
  });

  it('potwierdzPodpis() woła DokumentyApi.podpisz, dokłada nową wersję i utrzymuje porządek malejący', async () => {
    const nowaWersja: WersjaDokumentu = {
      id: 'wd-3',
      dokumentId: 'd-2',
      wersja: 3,
      podpisana: true,
      dataUtworzenia: '2026-10-25',
      autor: 'Katarzyna Zielińska',
      statusPodpisu: 'poprawny',
    };
    dokumentyApiStub.podpisz.mockReturnValue(of(nowaWersja));
    const fixture = await wczytaj();

    fixture.componentInstance['podpisz'](wersje[1]);
    fixture.componentInstance['certyfikatWybrany'].set('cert-1');
    fixture.componentInstance['potwierdzPodpis']();

    expect(dokumentyApiStub.podpisz).toHaveBeenCalledWith('d-2', 'cert-1');
    expect(fixture.componentInstance['wersje']().map((w: WersjaDokumentu) => w.wersja)).toEqual([
      3, 2, 1,
    ]);
    expect(fixture.componentInstance['dokument']()?.aktualnaWersja).toBe(3);
    expect(fixture.componentInstance['certPodpisWidoczny']()).toBe(false);
  });

  it('błąd 400 z backendu przy podpisywaniu ustawia komunikat błędu i nie zamyka dialogu', async () => {
    dokumentyApiStub.podpisz.mockReturnValue(
      throwError(
        () => new HttpErrorResponse({ status: 400, error: { komunikat: 'certyfikat wygasły' } }),
      ),
    );
    const fixture = await wczytaj();

    fixture.componentInstance['podpisz'](wersje[1]);
    fixture.componentInstance['certyfikatWybrany'].set('cert-2');
    fixture.componentInstance['potwierdzPodpis']();

    expect(fixture.componentInstance['certBladPodpisu']()).toBe('certyfikat wygasły');
    expect(fixture.componentInstance['certPodpisWidoczny']()).toBe(true);
  });
});
