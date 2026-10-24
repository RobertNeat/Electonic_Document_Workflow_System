import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import type {
  Przesylka,
  Sprawa,
  Dokument,
  Obywatel,
} from '@electronic-document-workflow/document-shared-dto';
import { KorespondencjaSzczegoly } from './korespondencja-szczegoly';
import { PrzesylkiApi } from '../../../core/api/przesylki.api';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { ObywateleApi } from '../../../core/api/obywatele.api';
import { DokumentyApi } from '../../../core/api/dokumenty.api';

describe('KorespondencjaSzczegoly', () => {
  let przesylkiApiStub: { getById: ReturnType<typeof vi.fn> };
  let sprawyApiStub: { getById: ReturnType<typeof vi.fn> };
  let obywateleApiStub: { getById: ReturnType<typeof vi.fn> };
  let dokumentyApiStub: { getById: ReturnType<typeof vi.fn> };

  const przesylka: Przesylka = {
    id: 'p-1',
    sprawaId: 's-1',
    dokumentId: 'd-1',
    obywatelId: 'ob-1',
    kanal: 'email',
    status: 'nadane',
    numerReferencyjny: 'MAIL/2026/00001',
    dataNadania: '2026-10-01',
    dataStatusu: '2026-10-01',
    kierunek: 'wychodzaca',
    historiaStatusow: [{ status: 'nadane', data: '2026-10-01' }],
  };

  beforeEach(() => {
    przesylkiApiStub = { getById: vi.fn().mockReturnValue(of(przesylka)) };
    sprawyApiStub = { getById: vi.fn().mockReturnValue(of({ id: 's-1' } as Sprawa)) };
    obywateleApiStub = { getById: vi.fn().mockReturnValue(of({ id: 'ob-1' } as Obywatel)) };
    dokumentyApiStub = { getById: vi.fn().mockReturnValue(of({ id: 'd-1' } as Dokument)) };

    TestBed.configureTestingModule({
      imports: [KorespondencjaSzczegoly],
      providers: [
        provideRouter([]),
        { provide: PrzesylkiApi, useValue: przesylkiApiStub },
        { provide: SprawyApi, useValue: sprawyApiStub },
        { provide: ObywateleApi, useValue: obywateleApiStub },
        { provide: DokumentyApi, useValue: dokumentyApiStub },
      ],
    });
  });

  beforeEach(() => {
    // timer(0, ...) RxJS harmonogramuje nawet "zerowe" opóźnienie przez asyncScheduler (nie
    // synchronicznie jak of()) - fake timers + advanceTimersByTimeAsync(0) są potrzebne we
    // WSZYSTKICH testach tego komponentu, nie tylko w teście pollingu.
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('wczytuje przesyłkę i powiązane dane, wypełnia stan "ok"', async () => {
    const fixture = TestBed.createComponent(KorespondencjaSzczegoly);
    fixture.componentRef.setInput('id', 'p-1');
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(0);
    fixture.detectChanges();

    expect(fixture.componentInstance['status']()).toBe('ok');
    expect(fixture.componentInstance['przesylka']()).toEqual(przesylka);
    expect(sprawyApiStub.getById).toHaveBeenCalledWith('s-1');
    expect(dokumentyApiStub.getById).toHaveBeenCalledWith('d-1');
    expect(obywateleApiStub.getById).toHaveBeenCalledWith('ob-1');
  });

  it('404 na pierwszym pobraniu ustawia stan "nieznaleziona"', async () => {
    przesylkiApiStub.getById.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );

    const fixture = TestBed.createComponent(KorespondencjaSzczegoly);
    fixture.componentRef.setInput('id', 'p-brak');
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(0);
    fixture.detectChanges();

    expect(fixture.componentInstance['status']()).toBe('nieznaleziona');
  });

  it('błąd sieci na pierwszym pobraniu ustawia stan "blad" z komunikatem', async () => {
    przesylkiApiStub.getById.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0 })),
    );

    const fixture = TestBed.createComponent(KorespondencjaSzczegoly);
    fixture.componentRef.setInput('id', 'p-1');
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(0);
    fixture.detectChanges();

    expect(fixture.componentInstance['status']()).toBe('blad');
    expect(fixture.componentInstance['komunikatBledu']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
  });

  it('odpytuje GET /api/przesylki/:id ponownie po interwale pollingu, i zatrzymuje po zniszczeniu komponentu', async () => {
    const fixture = TestBed.createComponent(KorespondencjaSzczegoly);
    fixture.componentRef.setInput('id', 'p-1');
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(0);

    expect(przesylkiApiStub.getById).toHaveBeenCalledTimes(1);

    await vi.advanceTimersByTimeAsync(8000);
    expect(przesylkiApiStub.getById).toHaveBeenCalledTimes(2);

    fixture.destroy();
    await vi.advanceTimersByTimeAsync(16000);
    expect(przesylkiApiStub.getById).toHaveBeenCalledTimes(2);
  });

  it('zachowuje ostatnie dobre dane, gdy poźniejszy tik pollingu zawiedzie (nie zastępuje widoku błędem)', async () => {
    przesylkiApiStub.getById
      .mockReturnValueOnce(of(przesylka))
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = TestBed.createComponent(KorespondencjaSzczegoly);
    fixture.componentRef.setInput('id', 'p-1');
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(0);
    expect(fixture.componentInstance['status']()).toBe('ok');

    await vi.advanceTimersByTimeAsync(8000);

    expect(fixture.componentInstance['status']()).toBe('ok');
    expect(fixture.componentInstance['przesylka']()).toEqual(przesylka);
  });
});
