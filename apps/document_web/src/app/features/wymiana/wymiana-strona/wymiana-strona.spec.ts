import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import type { WpisHistoriiWymiany } from '@electronic-document-workflow/document-shared-dto';
import { WymianaStrona } from './wymiana-strona';
import { WymianaApi } from '../../../core/api/wymiana.api';

// p-tabs (PrimeNG) obserwuje zmianę rozmiaru paska zakładek przez ResizeObserver w
// ngAfterViewInit - niedostępne w jsdom (środowisko testowe), stub wystarczający dla testów.
class ResizeObserverStub {
  observe(): void {}
  unobserve(): void {}
  disconnect(): void {}
}

describe('WymianaStrona', () => {
  beforeAll(() => {
    (globalThis as { ResizeObserver?: unknown }).ResizeObserver = ResizeObserverStub;
  });

  let apiStub: { historiaXml: ReturnType<typeof vi.fn>; historiaUbl: ReturnType<typeof vi.fn> };

  const historiaXml: WpisHistoriiWymiany[] = [
    {
      id: 'hx-1',
      nazwaPliku: 'komunikat.xml',
      kierunek: 'import',
      data: '2026-10-20T09:14:00Z',
      status: 'sukces',
      liczbaRekordow: 1,
    },
  ];
  const historiaUbl: WpisHistoriiWymiany[] = [
    {
      id: 'hu-1',
      nazwaPliku: 'faktura.xml',
      kierunek: 'import',
      data: '2026-10-19T10:05:00Z',
      status: 'sukces',
      liczbaRekordow: 1,
    },
  ];

  beforeEach(() => {
    apiStub = {
      historiaXml: vi.fn().mockReturnValue(of(historiaXml)),
      historiaUbl: vi.fn().mockReturnValue(of(historiaUbl)),
    };

    TestBed.configureTestingModule({
      imports: [WymianaStrona],
      providers: [provideRouter([]), { provide: WymianaApi, useValue: apiStub }],
    });
  });

  it('wczytuje historię XML i UBL przy montowaniu', () => {
    const fixture = TestBed.createComponent(WymianaStrona);
    fixture.detectChanges();

    expect(apiStub.historiaXml).toHaveBeenCalled();
    expect(apiStub.historiaUbl).toHaveBeenCalled();
    expect(fixture.componentInstance['historiaXml']()).toEqual(historiaXml);
    expect(fixture.componentInstance['historiaUbl']()).toEqual(historiaUbl);
    expect(fixture.componentInstance['wczytywanieXml']()).toBe(false);
    expect(fixture.componentInstance['wczytywanieUbl']()).toBe(false);
  });

  it('pokazuje komunikat błędu, gdy historiaXml() zawiedzie, bez wpływu na historię UBL', () => {
    apiStub.historiaXml.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = TestBed.createComponent(WymianaStrona);
    fixture.detectChanges();

    expect(fixture.componentInstance['komunikatBleduXml']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
    expect(fixture.componentInstance['historiaUbl']()).toEqual(historiaUbl);
  });

  it('pokazuje komunikat błędu, gdy historiaUbl() zawiedzie', () => {
    apiStub.historiaUbl.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));

    const fixture = TestBed.createComponent(WymianaStrona);
    fixture.detectChanges();

    expect(fixture.componentInstance['komunikatBleduUbl']()).toBe(
      'Brak uprawnień do wykonania tej operacji.',
    );
  });
});
