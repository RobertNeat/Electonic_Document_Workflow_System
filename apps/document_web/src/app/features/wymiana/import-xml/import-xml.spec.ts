import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import type { WynikWalidacjiXml } from '@electronic-document-workflow/document-shared-dto';
import { ImportXml } from './import-xml';
import { WymianaApi } from '../../../core/api/wymiana.api';

describe('ImportXml', () => {
  let apiStub: { importujXml: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    apiStub = { importujXml: vi.fn() };

    TestBed.configureTestingModule({
      imports: [ImportXml],
      providers: [provideRouter([]), { provide: WymianaApi, useValue: apiStub }],
    });
  });

  function uploadEvent(nazwa: string) {
    const plik = { name: nazwa } as File;
    return { files: [plik] } as never;
  }

  it('onUpload() woła importujXml() i pokazuje podgląd mapowania przy sukces=true', () => {
    const wynik: WynikWalidacjiXml = {
      sukces: true,
      bledy: [],
      sprawaZmapowana: { znakSprawy: 'WO.6220.14.2026', tytul: 'Sprawa testowa' },
    };
    apiStub.importujXml.mockReturnValue(of(wynik));

    const fixture = TestBed.createComponent(ImportXml);
    fixture.detectChanges();
    fixture.componentInstance['onUpload'](uploadEvent('komunikat.xml'));

    expect(apiStub.importujXml).toHaveBeenCalled();
    expect(fixture.componentInstance['wynik']()).toEqual(wynik);
    expect(fixture.componentInstance['mapowanie']()).toEqual([
      { pole: 'Znak sprawy', wartosc: 'WO.6220.14.2026' },
      { pole: 'Tytuł sprawy', wartosc: 'Sprawa testowa' },
    ]);
  });

  it('onUpload() pokazuje błędy walidacji, gdy backend zwraca 200 z sukces=false', () => {
    const wynik: WynikWalidacjiXml = {
      sukces: false,
      bledy: [{ element: 'znakSprawy', opis: 'Element wymagany' }],
    };
    apiStub.importujXml.mockReturnValue(of(wynik));

    const fixture = TestBed.createComponent(ImportXml);
    fixture.detectChanges();
    fixture.componentInstance['onUpload'](uploadEvent('niepoprawny.xml'));

    expect(fixture.componentInstance['wynik']()).toEqual(wynik);
    expect(fixture.componentInstance['komunikatBledu']()).toBeNull();
  });

  it('onUpload() pokazuje komunikat błędu sieciowego i nie crashuje', () => {
    apiStub.importujXml.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 400 })));

    const fixture = TestBed.createComponent(ImportXml);
    fixture.detectChanges();
    fixture.componentInstance['onUpload'](uploadEvent('plik.txt'));

    expect(fixture.componentInstance['komunikatBledu']()).toBeTruthy();
    expect(fixture.componentInstance['wynik']()).toBeNull();
  });

  it('wyczysc() resetuje stan', () => {
    apiStub.importujXml.mockReturnValue(of({ sukces: true, bledy: [] } as WynikWalidacjiXml));

    const fixture = TestBed.createComponent(ImportXml);
    fixture.detectChanges();
    fixture.componentInstance['onUpload'](uploadEvent('komunikat.xml'));
    fixture.componentInstance['wyczysc']();

    expect(fixture.componentInstance['nazwaPliku']()).toBeNull();
    expect(fixture.componentInstance['wynik']()).toBeNull();
  });
});
