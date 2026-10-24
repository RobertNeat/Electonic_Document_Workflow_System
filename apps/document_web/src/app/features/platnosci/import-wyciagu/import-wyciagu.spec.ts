import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import localePl from '@angular/common/locales/pl';
import type { FileUploadHandlerEvent } from 'primeng/fileupload';
import type { WynikImportuCamt053 } from '@electronic-document-workflow/document-shared-dto';
import { ImportWyciagu } from './import-wyciagu';
import { PlatnosciStore } from '../data/platnosci-store';

// CurrencyPipe w szablonie wymaga zarejestrowanych danych locale "pl" - w aplikacji rejestrowane
// raz w app.config.ts, ale ten spec nie ładuje app.config.
registerLocaleData(localePl);

describe('ImportWyciagu', () => {
  let storeStub: { importujWyciag: ReturnType<typeof vi.fn> };

  const zdarzenie = (plik: File): FileUploadHandlerEvent =>
    ({ files: [plik] }) as unknown as FileUploadHandlerEvent;

  beforeEach(() => {
    storeStub = { importujWyciag: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ImportWyciagu],
      providers: [provideRouter([]), { provide: PlatnosciStore, useValue: storeStub }],
    });
  });

  it('importujPlik() wysyła realny plik (nie tylko nazwę) i pokazuje realny wynik parsowania', async () => {
    const wynikApi: WynikImportuCamt053 = {
      liczbaTransakcji: 5,
      liczbaDopasowanychAutomatycznie: 3,
      liczbaDoRekoncyliacji: 2,
      utworzoneWplaty: [],
    };
    storeStub.importujWyciag.mockResolvedValue({ nazwaPliku: 'wyciag.xml', wynik: wynikApi });
    const plik = new File(['<Document></Document>'], 'wyciag.xml', { type: 'application/xml' });

    const fixture = TestBed.createComponent(ImportWyciagu);
    fixture.detectChanges();

    fixture.componentInstance.importujPlik(zdarzenie(plik));
    await Promise.resolve();
    fixture.detectChanges();

    expect(storeStub.importujWyciag).toHaveBeenCalledWith(plik);
    expect(fixture.componentInstance['wynikImportu']()).toEqual({
      nazwaPliku: 'wyciag.xml',
      wynik: wynikApi,
    });
  });

  it('importujPlik() przy błędzie (np. plik nie jest XML-em) pokazuje komunikat, nie wynik', async () => {
    storeStub.importujWyciag.mockResolvedValue('Przesłany plik nie jest plikiem XML');
    const plik = new File(['nie-xml'], 'wyciag.txt');

    const fixture = TestBed.createComponent(ImportWyciagu);
    fixture.detectChanges();

    fixture.componentInstance.importujPlik(zdarzenie(plik));
    await Promise.resolve();
    fixture.detectChanges();

    expect(fixture.componentInstance['blad']()).toBe('Przesłany plik nie jest plikiem XML');
    expect(fixture.componentInstance['wynikImportu']()).toBeNull();
  });

  it('nowyImport() czyści wynik i komunikat błędu', async () => {
    storeStub.importujWyciag.mockResolvedValue('błąd');
    const plik = new File(['nie-xml'], 'wyciag.txt');

    const fixture = TestBed.createComponent(ImportWyciagu);
    fixture.detectChanges();
    fixture.componentInstance.importujPlik(zdarzenie(plik));
    await Promise.resolve();

    fixture.componentInstance.nowyImport();

    expect(fixture.componentInstance['wynikImportu']()).toBeNull();
    expect(fixture.componentInstance['blad']()).toBeNull();
  });
});
