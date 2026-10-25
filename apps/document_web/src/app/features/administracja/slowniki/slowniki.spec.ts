import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import type {
  DostawcaWysylki,
  PozycjaJrwa,
  StawkaOplaty,
  TypDokumentu,
  Uzytkownik,
} from '@electronic-document-workflow/document-shared-dto';
import { Slowniki } from './slowniki';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { AdministracjaApi } from '../../../core/api/administracja.api';
import { AuthService } from '../../../core/services/auth.service';

// p-tabs (PrimeNG) obserwuje zmianę rozmiaru paska zakładek przez ResizeObserver w
// ngAfterViewInit - niedostępne w jsdom (środowisko testowe), stub wystarczający dla testów.
class ResizeObserverStub {
  observe(): void {}
  unobserve(): void {}
  disconnect(): void {}
}

describe('Slowniki', () => {
  beforeAll(() => {
    (globalThis as { ResizeObserver?: unknown }).ResizeObserver = ResizeObserverStub;
  });

  let sprawyApiStub: {
    jrwa: ReturnType<typeof vi.fn>;
    typyDokumentow: ReturnType<typeof vi.fn>;
    aktualizujJrwa: ReturnType<typeof vi.fn>;
    aktualizujTypDokumentu: ReturnType<typeof vi.fn>;
  };
  let apiStub: {
    dostawcyWysylki: ReturnType<typeof vi.fn>;
    stawkiOplat: ReturnType<typeof vi.fn>;
    aktualizujDostawceWysylki: ReturnType<typeof vi.fn>;
    aktualizujStawkeOplaty: ReturnType<typeof vi.fn>;
  };

  const jrwa: PozycjaJrwa[] = [
    { symbol: '6220', haslo: 'Ochrona środowiska', kategoriaArchiwalna: 'B10' },
  ];
  const typyDokumentow: TypDokumentu[] = [{ id: 'td-1', nazwa: 'Decyzja', opis: 'Opis' }];
  const dostawcy: DostawcaWysylki[] = [
    { id: 'd-1', nazwa: 'e-Doręczenia', typ: 'e-doreczenia', aktywny: true },
  ];
  const stawki: StawkaOplaty[] = [{ id: 's-1', nazwa: 'Opłata skarbowa', kwota: 17 }];

  function skonfigurujTestBed(role: string[] = ['administrator']) {
    sprawyApiStub = {
      jrwa: vi.fn().mockReturnValue(of(jrwa)),
      typyDokumentow: vi.fn().mockReturnValue(of(typyDokumentow)),
      aktualizujJrwa: vi.fn(),
      aktualizujTypDokumentu: vi.fn(),
    };
    apiStub = {
      dostawcyWysylki: vi.fn().mockReturnValue(of(dostawcy)),
      stawkiOplat: vi.fn().mockReturnValue(of(stawki)),
      aktualizujDostawceWysylki: vi.fn(),
      aktualizujStawkeOplaty: vi.fn(),
    };
    const authStub = { uzytkownik: signal({ role } as unknown as Uzytkownik) };
    TestBed.configureTestingModule({
      imports: [Slowniki],
      providers: [
        { provide: SprawyApi, useValue: sprawyApiStub },
        { provide: AdministracjaApi, useValue: apiStub },
        { provide: AuthService, useValue: authStub },
      ],
    });
  }

  beforeEach(() => skonfigurujTestBed());

  it('wczytuje wszystkie cztery słowniki przy montowaniu', () => {
    const fixture = TestBed.createComponent(Slowniki);
    fixture.detectChanges();

    expect(fixture.componentInstance.jrwa()).toEqual(jrwa);
    expect(fixture.componentInstance.typyDokumentow()).toEqual(typyDokumentow);
    expect(fixture.componentInstance.dostawcyWysylki()).toEqual(dostawcy);
    expect(fixture.componentInstance.stawkiOplat()).toEqual(stawki);
    expect(fixture.componentInstance.wczytywanie()).toBe(false);
  });

  it('pokazuje komunikat błędu, gdy jeden ze słowników zawiedzie', () => {
    sprawyApiStub.jrwa.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = TestBed.createComponent(Slowniki);
    fixture.detectChanges();

    expect(fixture.componentInstance.komunikatBledu()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
  });

  it('zapiszJrwa() woła aktualizujJrwa() i aktualizuje wiersz z odpowiedzią serwera', () => {
    const zaktualizowana: PozycjaJrwa = { ...jrwa[0], haslo: 'Nowe hasło' };
    sprawyApiStub.aktualizujJrwa.mockReturnValue(of(zaktualizowana));

    const fixture = TestBed.createComponent(Slowniki);
    fixture.detectChanges();
    fixture.componentInstance.otworzEdycjeJrwa(jrwa[0]);
    fixture.componentInstance.zapiszJrwa();

    expect(sprawyApiStub.aktualizujJrwa).toHaveBeenCalledWith('6220', jrwa[0]);
    expect(fixture.componentInstance.jrwa()[0]).toEqual(zaktualizowana);
    expect(fixture.componentInstance.widocznyDialog()).toBeNull();
  });

  it('zapiszDostawce() pokazuje komunikat błędu i nie zamyka dialogu, gdy zapis zawiedzie', () => {
    apiStub.aktualizujDostawceWysylki.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403 })),
    );

    const fixture = TestBed.createComponent(Slowniki);
    fixture.detectChanges();
    fixture.componentInstance.otworzEdycjeDostawcy(dostawcy[0]);
    fixture.componentInstance.zapiszDostawce();

    expect(fixture.componentInstance.edycjaBlad()).toBe(
      'Brak uprawnień do wykonania tej operacji.',
    );
    expect(fixture.componentInstance.widocznyDialog()).toBe('dostawca');
  });

  it('zapiszStawke() woła aktualizujStawkeOplaty() i aktualizuje wiersz z odpowiedzią serwera', () => {
    const zaktualizowana: StawkaOplaty = { ...stawki[0], kwota: 25 };
    apiStub.aktualizujStawkeOplaty.mockReturnValue(of(zaktualizowana));

    const fixture = TestBed.createComponent(Slowniki);
    fixture.detectChanges();
    fixture.componentInstance.otworzEdycjeStawki(stawki[0]);
    fixture.componentInstance.zapiszStawke();

    expect(apiStub.aktualizujStawkeOplaty).toHaveBeenCalledWith('s-1', stawki[0]);
    expect(fixture.componentInstance.stawkiOplat()[0]).toEqual(zaktualizowana);
  });

  it('czyAdministrator() jest false dla roli innej niż administrator', () => {
    skonfigurujTestBed(['referent']);
    const fixture = TestBed.createComponent(Slowniki);
    fixture.detectChanges();

    expect(fixture.componentInstance.czyAdministrator()).toBe(false);
  });
});
