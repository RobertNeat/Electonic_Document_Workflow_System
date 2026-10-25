import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import type { WynikWalidacjiUbl } from '@electronic-document-workflow/document-shared-dto';
import { ImportUbl } from './import-ubl';
import { WymianaApi } from '../../../core/api/wymiana.api';

describe('ImportUbl', () => {
  let apiStub: { importujUbl: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    apiStub = { importujUbl: vi.fn() };

    TestBed.configureTestingModule({
      imports: [ImportUbl],
      providers: [provideRouter([]), { provide: WymianaApi, useValue: apiStub }],
    });
  });

  function uploadEvent(nazwa: string) {
    const plik = { name: nazwa } as File;
    return { files: [plik] } as never;
  }

  it('onUpload() woła importujUbl() i pokazuje dane faktury przy sukces=true', () => {
    const wynik: WynikWalidacjiUbl = {
      sukces: true,
      bledy: [],
      faktura: {
        numer: 'FV/2026/10/0231',
        kontrahent: 'Budimex Serwis Sp. z o.o.',
        nip: '5261234567',
        kwota: 4500,
        pozycje: [{ nazwa: 'Przegląd', ilosc: 1, cenaJednostkowa: 2500, wartosc: 3075 }],
        numerReferencyjnyNaleznosci: 'FK/2026/0089',
      },
    };
    apiStub.importujUbl.mockReturnValue(of(wynik));

    const fixture = TestBed.createComponent(ImportUbl);
    fixture.detectChanges();
    fixture.componentInstance['onUpload'](uploadEvent('faktura.xml'));

    expect(apiStub.importujUbl).toHaveBeenCalled();
    expect(fixture.componentInstance['wynik']()).toEqual(wynik);
  });

  it('onUpload() pokazuje błędy walidacji, gdy backend zwraca 200 z sukces=false', () => {
    const wynik: WynikWalidacjiUbl = {
      sukces: false,
      bledy: [{ element: 'cbc:InvoiceTypeCode', opis: 'Element wymagany' }],
    };
    apiStub.importujUbl.mockReturnValue(of(wynik));

    const fixture = TestBed.createComponent(ImportUbl);
    fixture.detectChanges();
    fixture.componentInstance['onUpload'](uploadEvent('niepoprawna.xml'));

    expect(fixture.componentInstance['wynik']()).toEqual(wynik);
    expect(fixture.componentInstance['komunikatBledu']()).toBeNull();
  });

  it('onUpload() pokazuje komunikat błędu sieciowego i nie crashuje', () => {
    apiStub.importujUbl.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));

    const fixture = TestBed.createComponent(ImportUbl);
    fixture.detectChanges();
    fixture.componentInstance['onUpload'](uploadEvent('faktura.xml'));

    expect(fixture.componentInstance['komunikatBledu']()).toBe(
      'Brak uprawnień do wykonania tej operacji.',
    );
    expect(fixture.componentInstance['wynik']()).toBeNull();
  });
});
