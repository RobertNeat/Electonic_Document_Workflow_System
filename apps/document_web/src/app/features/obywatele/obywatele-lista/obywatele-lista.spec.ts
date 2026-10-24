import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import type { Obywatel } from '@electronic-document-workflow/document-shared-dto';
import { ObywateleLista } from './obywatele-lista';
import { ObywateleDaneService } from '../obywatele-dane.service';

describe('ObywateleLista', () => {
  let daneStub: { szukaj: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    daneStub = { szukaj: vi.fn().mockReturnValue(of([])) };
    TestBed.configureTestingModule({
      imports: [ObywateleLista],
      providers: [provideRouter([]), { provide: ObywateleDaneService, useValue: daneStub }],
    });
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('wczytuje wyniki wyszukiwania po debounce i wypełnia obywatele()', async () => {
    const wynik: Obywatel[] = [{ id: 'ob-1' } as Obywatel];
    daneStub.szukaj.mockReturnValue(of(wynik));

    const fixture = TestBed.createComponent(ObywateleLista);
    fixture.detectChanges();

    await vi.advanceTimersByTimeAsync(300);

    expect(daneStub.szukaj).toHaveBeenCalledWith('');
    expect(fixture.componentInstance['obywatele']()).toEqual(wynik);
    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
    expect(fixture.componentInstance['komunikatBledu']()).toBeNull();
  });

  it('ustawia komunikatBledu() przy błędzie wyszukiwania, bez wyjątku nieobsłużonego', async () => {
    daneStub.szukaj.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = TestBed.createComponent(ObywateleLista);
    fixture.detectChanges();

    await vi.advanceTimersByTimeAsync(300);

    expect(fixture.componentInstance['komunikatBledu']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
    expect(fixture.componentInstance['obywatele']()).toEqual([]);
    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
  });

  it('wpisanie nowego zapytania wywołuje nowe wyszukiwanie (po debounce)', async () => {
    const fixture = TestBed.createComponent(ObywateleLista);
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    daneStub.szukaj.mockClear();

    fixture.componentInstance['zapytanie'].set('Nowak');
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);

    expect(daneStub.szukaj).toHaveBeenCalledWith('Nowak');
  });
});
