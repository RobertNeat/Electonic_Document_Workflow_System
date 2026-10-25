import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import type {
  Sprawa,
  StatusAkcjiZbiorowej,
  StronaWynikow,
  SzablonJasper,
  UruchomAkcjeZbiorowaResponse,
} from '@electronic-document-workflow/document-shared-dto';
import { AkcjeZbiorowe } from './akcje-zbiorowe';
import { AkcjeZbioroweApi } from '../../../core/api/akcje-zbiorowe.api';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { DokumentyApi } from '../../../core/api/dokumenty.api';

describe('AkcjeZbiorowe', () => {
  let akcjeApiStub: { uruchom: ReturnType<typeof vi.fn>; status: ReturnType<typeof vi.fn> };
  let sprawyApiStub: { szukaj: ReturnType<typeof vi.fn> };
  let dokumentyApiStub: { szablony: ReturnType<typeof vi.fn> };

  const sprawy: Sprawa[] = [
    {
      id: 's-1',
      znakSprawy: 'WO.6220.1.2026',
      tytul: 'Sprawa 1',
      jrwa: '6220',
      definicjaObieguId: 'd-1',
      stan: 'W toku',
      obywatelId: 'ob-1',
      wydzial: 'WO',
      referent: 'Jan Kowalski',
      dataWszczecia: '2026-01-01',
      kategoriaArchiwalna: 'A',
      zamknieta: false,
    },
  ];
  const stronaSpraw: StronaWynikow<Sprawa> = {
    dane: sprawy,
    strona: 0,
    rozmiarStrony: 200,
    liczbaWszystkich: 1,
  };
  const szablony: SzablonJasper[] = [
    {
      id: 'sz-1',
      typDokumentuId: 'td-1',
      nazwa: 'decyzja.jrxml',
      wersja: 1,
      dataWgrania: '2026-01-01',
      autor: 'Admin',
    },
  ];

  beforeEach(() => {
    akcjeApiStub = { uruchom: vi.fn(), status: vi.fn() };
    sprawyApiStub = { szukaj: vi.fn().mockReturnValue(of(stronaSpraw)) };
    dokumentyApiStub = { szablony: vi.fn().mockReturnValue(of(szablony)) };

    TestBed.configureTestingModule({
      imports: [AkcjeZbiorowe],
      providers: [
        provideRouter([]),
        { provide: AkcjeZbioroweApi, useValue: akcjeApiStub },
        { provide: SprawyApi, useValue: sprawyApiStub },
        { provide: DokumentyApi, useValue: dokumentyApiStub },
      ],
    });
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('wczytuje sprawy i szablony przy montowaniu', () => {
    const fixture = TestBed.createComponent(AkcjeZbiorowe);
    fixture.detectChanges();

    expect(sprawyApiStub.szukaj).toHaveBeenCalledWith({ rozmiarStrony: 200 });
    expect(dokumentyApiStub.szablony).toHaveBeenCalled();
    expect(fixture.componentInstance['sprawy']()).toEqual(sprawy);
    expect(fixture.componentInstance['szablony']()).toEqual(szablony);
  });

  it('uruchomZmianeStanu() startuje akcję, pollinguje status i zatrzymuje się po zakonczona', async () => {
    const response: UruchomAkcjeZbiorowaResponse = { akcjaId: 'akcja-1' };
    akcjeApiStub.uruchom.mockReturnValue(of(response));

    const trwajaca: StatusAkcjiZbiorowej = {
      akcjaId: 'akcja-1',
      typAkcji: 'ZMIANA_STANU',
      typCelu: 'SPRAWA',
      status: 'trwajaca',
      postep: 50,
      lacznie: 1,
      przetworzone: 0,
      bledne: 0,
      wynikiElementow: [],
    };
    const zakonczona: StatusAkcjiZbiorowej = {
      ...trwajaca,
      status: 'zakonczona',
      postep: 100,
      przetworzone: 1,
      wynikiElementow: [{ idCelu: 's-1', status: 'sukces' }],
    };
    akcjeApiStub.status.mockReturnValueOnce(of(trwajaca)).mockReturnValueOnce(of(zakonczona));

    const fixture = TestBed.createComponent(AkcjeZbiorowe);
    fixture.detectChanges();

    fixture.componentInstance['wybraneZmianaStanu'].set(sprawy);
    fixture.componentInstance['nowyStan'] = 'W toku';
    fixture.componentInstance['uruchomZmianeStanu']();

    expect(akcjeApiStub.uruchom).toHaveBeenCalledWith({
      typAkcji: 'ZMIANA_STANU',
      typCelu: 'SPRAWA',
      idCelow: ['s-1'],
      parametry: { nowyStan: 'W toku' },
    });

    await vi.advanceTimersByTimeAsync(0);
    expect(akcjeApiStub.status).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance['przebiegi']()['ZMIANA_STANU'].status).toEqual(trwajaca);

    await vi.advanceTimersByTimeAsync(1200);
    expect(akcjeApiStub.status).toHaveBeenCalledTimes(2);
    expect(fixture.componentInstance['przebiegi']()['ZMIANA_STANU'].status).toEqual(zakonczona);

    // Polling musi zatrzymać się po "zakonczona" - kolejne tiki nie wywołują status() ponownie.
    await vi.advanceTimersByTimeAsync(3600);
    expect(akcjeApiStub.status).toHaveBeenCalledTimes(2);
  });

  it('pokazuje komunikat błędu, gdy uruchomienie akcji zawiedzie (np. 403)', () => {
    akcjeApiStub.uruchom.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));

    const fixture = TestBed.createComponent(AkcjeZbiorowe);
    fixture.detectChanges();

    fixture.componentInstance['wybraneWysylka'].set(sprawy);
    fixture.componentInstance['kanalWysylki'] = 'email';
    fixture.componentInstance['uruchomWysylke']();

    expect(fixture.componentInstance['przebiegi']()['WYSYLKA_PISM'].blad).toBe(
      'Brak uprawnień do wykonania tej operacji.',
    );
    expect(akcjeApiStub.status).not.toHaveBeenCalled();
  });

  it('uruchomGenerowanie() wysyła szablonId w parametrach', async () => {
    akcjeApiStub.uruchom.mockReturnValue(of({ akcjaId: 'akcja-2' }));
    akcjeApiStub.status.mockReturnValue(
      of({
        akcjaId: 'akcja-2',
        typAkcji: 'GENEROWANIE_DOKUMENTOW',
        typCelu: 'SPRAWA',
        status: 'zakonczona',
        postep: 100,
        lacznie: 1,
        przetworzone: 1,
        bledne: 0,
        wynikiElementow: [],
      } satisfies StatusAkcjiZbiorowej),
    );

    const fixture = TestBed.createComponent(AkcjeZbiorowe);
    fixture.detectChanges();

    fixture.componentInstance['wybraneGenerowanie'].set(sprawy);
    fixture.componentInstance['szablonId'] = 'sz-1';
    fixture.componentInstance['uruchomGenerowanie']();

    expect(akcjeApiStub.uruchom).toHaveBeenCalledWith({
      typAkcji: 'GENEROWANIE_DOKUMENTOW',
      typCelu: 'SPRAWA',
      idCelow: ['s-1'],
      parametry: { szablonId: 'sz-1' },
    });

    await vi.advanceTimersByTimeAsync(0);
  });
});
