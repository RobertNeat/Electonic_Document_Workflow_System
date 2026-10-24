import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NEVER, of } from 'rxjs';
import type {
  Przesylka,
  Sprawa,
  Obywatel,
} from '@electronic-document-workflow/document-shared-dto';
import { KorespondencjaStrona } from './korespondencja-strona';
import { KorespondencjaStore } from '../korespondencja.store';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { ObywateleApi } from '../../../core/api/obywatele.api';

// p-tabs (PrimeNG) obserwuje zmianę rozmiaru paska zakładek przez ResizeObserver w
// ngAfterViewInit - niedostępne w jsdom (środowisko testowe), stub wystarczający dla testów.
class ResizeObserverStub {
  observe(): void {}
  unobserve(): void {}
  disconnect(): void {}
}

describe('KorespondencjaStrona', () => {
  beforeAll(() => {
    (globalThis as { ResizeObserver?: unknown }).ResizeObserver = ResizeObserverStub;
  });

  let storeStub: {
    przesylki: ReturnType<typeof vi.fn>;
    loading: ReturnType<typeof vi.fn>;
    blad: ReturnType<typeof vi.fn>;
    szukaj: ReturnType<typeof vi.fn>;
  };
  let sprawyApiStub: { getById: ReturnType<typeof vi.fn> };
  let obywateleApiStub: { getById: ReturnType<typeof vi.fn> };

  const przesylki: Przesylka[] = [
    { id: 'p-1', sprawaId: 's-1', obywatelId: 'ob-1', kierunek: 'wychodzaca' } as Przesylka,
  ];

  beforeEach(() => {
    storeStub = {
      przesylki: vi.fn().mockReturnValue(przesylki),
      loading: vi.fn().mockReturnValue(false),
      blad: vi.fn().mockReturnValue(null),
      szukaj: vi.fn(),
    };
    sprawyApiStub = {
      getById: vi.fn().mockReturnValue(of({ id: 's-1', znakSprawy: 'OR.1.1.2026' } as Sprawa)),
    };
    obywateleApiStub = {
      getById: vi
        .fn()
        .mockReturnValue(
          of({ id: 'ob-1', typ: 'osoba_fizyczna', imie: 'Jan', nazwisko: 'Kowalski' } as Obywatel),
        ),
    };

    TestBed.configureTestingModule({
      imports: [KorespondencjaStrona],
      providers: [
        provideRouter([]),
        { provide: KorespondencjaStore, useValue: storeStub },
        { provide: SprawyApi, useValue: sprawyApiStub },
        { provide: ObywateleApi, useValue: obywateleApiStub },
      ],
    });
  });

  it('woła store.szukaj() z filtrem kierunku przy montowaniu', () => {
    const fixture = TestBed.createComponent(KorespondencjaStrona);
    fixture.detectChanges();

    expect(storeStub.szukaj).toHaveBeenCalledWith({
      kierunek: 'wychodzaca',
      status: null,
      kanal: null,
    });
  });

  it('zmiana kierunku wywołuje ponowne szukaj() z nowym filtrem', () => {
    const fixture = TestBed.createComponent(KorespondencjaStrona);
    fixture.detectChanges();
    storeStub.szukaj.mockClear();

    fixture.componentInstance.zmienKierunek('wplywajaca');
    fixture.detectChanges();

    expect(storeStub.szukaj).toHaveBeenCalledWith({
      kierunek: 'wplywajaca',
      status: null,
      kanal: null,
    });
  });

  it('rozwiązuje znakSprawy/nazwaObywatela przez API, nie przez surowe id', async () => {
    const fixture = TestBed.createComponent(KorespondencjaStrona);
    fixture.detectChanges();
    await Promise.resolve();
    await Promise.resolve();
    fixture.detectChanges();

    expect(sprawyApiStub.getById).toHaveBeenCalledWith('s-1');
    expect(obywateleApiStub.getById).toHaveBeenCalledWith('ob-1');
    expect(fixture.componentInstance.znakSprawy('s-1')).toBe('OR.1.1.2026');
    expect(fixture.componentInstance.nazwaObywatela('ob-1')).toContain('Kowalski');
  });

  it('przed rozwiązaniem nazwy pokazuje surowe id jako fallback', () => {
    // Lookupy po sprawie/obywatelu nie wracają jeszcze (NEVER) - cache jest pusty, więc
    // znakSprawy/nazwaObywatela muszą zwrócić surowe id, nie puste/undefined.
    sprawyApiStub.getById.mockReturnValue(NEVER);
    obywateleApiStub.getById.mockReturnValue(NEVER);

    const fixture = TestBed.createComponent(KorespondencjaStrona);
    fixture.detectChanges();

    expect(fixture.componentInstance.znakSprawy('s-1')).toBe('s-1');
    expect(fixture.componentInstance.nazwaObywatela('ob-1')).toBe('ob-1');
  });
});
