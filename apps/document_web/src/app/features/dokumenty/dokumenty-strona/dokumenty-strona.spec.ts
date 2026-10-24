import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { NEVER, of, throwError } from 'rxjs';
import type {
  DokumentListElement,
  TypDokumentu,
} from '@electronic-document-workflow/document-shared-dto';
import { DokumentyStrona } from './dokumenty-strona';
import { DokumentyApi } from '../../../core/api/dokumenty.api';
import { SprawyApi } from '../../../core/api/sprawy.api';

describe('DokumentyStrona', () => {
  let dokumentyApiStub: { lista: ReturnType<typeof vi.fn> };
  let sprawyApiStub: { typyDokumentow: ReturnType<typeof vi.fn> };

  const typy: TypDokumentu[] = [{ id: 'td-1', nazwa: 'Pełnomocnictwo', opis: '' }];

  const dokumenty: DokumentListElement[] = [
    {
      id: 'd-1',
      sprawaId: 's-1',
      typDokumentuId: 'td-1',
      nazwa: 'Pełnomocnictwo J. Kowalski',
      aktualnaWersja: 1,
      dataUtworzenia: '2026-09-03',
      znakSprawy: 'OR.1.1.2026',
      statusPodpisuAktualnej: 'brak',
      typDokumentuNazwa: 'Pełnomocnictwo',
    },
  ];

  beforeEach(() => {
    dokumentyApiStub = { lista: vi.fn().mockReturnValue(of(dokumenty)) };
    sprawyApiStub = { typyDokumentow: vi.fn().mockReturnValue(of(typy)) };

    TestBed.configureTestingModule({
      imports: [DokumentyStrona],
      providers: [
        provideRouter([]),
        { provide: DokumentyApi, useValue: dokumentyApiStub },
        { provide: SprawyApi, useValue: sprawyApiStub },
      ],
    });
  });

  it('wczytuje dokumenty i typy dokumentów przy montowaniu, bez filtrów', () => {
    const fixture = TestBed.createComponent(DokumentyStrona);
    fixture.detectChanges();

    expect(dokumentyApiStub.lista).toHaveBeenCalledWith({ sprawaId: null, typDokumentuId: null });
    expect(sprawyApiStub.typyDokumentow).toHaveBeenCalled();
    expect(fixture.componentInstance['dokumenty']()).toEqual(dokumenty);
    expect(fixture.componentInstance['typyDokumentow']()).toEqual(typy);
    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
  });

  it('ustawienie sprawaId jako wejścia routingu wywołuje ponowne lista() z filtrem sprawy', () => {
    const fixture = TestBed.createComponent(DokumentyStrona);
    fixture.componentRef.setInput('sprawaId', 's-1');
    fixture.detectChanges();

    expect(dokumentyApiStub.lista).toHaveBeenCalledWith({ sprawaId: 's-1', typDokumentuId: null });
  });

  it('zmiana filtra typu dokumentu wywołuje ponowne lista()', () => {
    const fixture = TestBed.createComponent(DokumentyStrona);
    fixture.detectChanges();
    dokumentyApiStub.lista.mockClear();

    fixture.componentInstance['typFiltr'].set('td-1');
    fixture.detectChanges();

    expect(dokumentyApiStub.lista).toHaveBeenCalledWith({ sprawaId: null, typDokumentuId: 'td-1' });
  });

  it('wyczyscFiltrSprawy() usuwa query param sprawaId z nawigacji', () => {
    const fixture = TestBed.createComponent(DokumentyStrona);
    fixture.detectChanges();
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate');

    fixture.componentInstance['wyczyscFiltrSprawy']();

    expect(navigateSpy).toHaveBeenCalledWith([], {
      queryParams: { sprawaId: null },
      queryParamsHandling: 'merge',
    });
  });

  it('pokazuje stan "wczytywanie" odrębny od pustej listy, dopóki żądanie nie wróci', () => {
    dokumentyApiStub.lista.mockReturnValue(NEVER);

    const fixture = TestBed.createComponent(DokumentyStrona);
    fixture.detectChanges();

    expect(fixture.componentInstance['wczytywanie']()).toBe(true);
    const tekst = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(tekst).not.toContain('Brak dokumentów spełniających kryteria filtrowania.');
  });

  it('pokazuje komunikat błędu HTTP, gdy lista() zawiedzie', () => {
    dokumentyApiStub.lista.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = TestBed.createComponent(DokumentyStrona);
    fixture.detectChanges();

    expect(fixture.componentInstance['komunikatBledu']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
    const tekst = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(tekst).toContain('Brak połączenia z serwerem');
  });
});
