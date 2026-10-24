import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { NEVER, of, throwError } from 'rxjs';
import type {
  DokumentListElement,
  Obywatel,
} from '@electronic-document-workflow/document-shared-dto';
import { ObywatelDokumenty } from './obywatel-dokumenty';
import { DokumentyApi } from '../../../core/api/dokumenty.api';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

describe('ObywatelDokumenty', () => {
  let apiStub: { lista: ReturnType<typeof vi.fn> };
  let kontekst: KartaObywatelaKontekst;

  const obywatel = { id: 'ob-1', typ: 'osoba_fizyczna' } as Obywatel;

  beforeEach(() => {
    apiStub = { lista: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ObywatelDokumenty],
      providers: [
        provideRouter([]),
        KartaObywatelaKontekst,
        { provide: DokumentyApi, useValue: apiStub },
      ],
    });
    kontekst = TestBed.inject(KartaObywatelaKontekst);
  });

  it('wczytuje dokumenty obywatela i wyświetla typDokumentuNazwa', () => {
    const dokumenty: DokumentListElement[] = [
      {
        id: 'd-2',
        sprawaId: 'spr-1',
        typDokumentuId: 'td-3',
        nazwa: 'Decyzja środowiskowa',
        aktualnaWersja: 2,
        dataUtworzenia: '2026-10-05',
        znakSprawy: 'WO.6220.14.2026',
        statusPodpisuAktualnej: 'poprawny',
        typDokumentuNazwa: 'Decyzja administracyjna',
      },
    ];
    apiStub.lista.mockReturnValue(of(dokumenty));
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelDokumenty);
    fixture.detectChanges();

    expect(apiStub.lista).toHaveBeenCalledWith({ obywatelId: 'ob-1' });
    expect(fixture.componentInstance['dokumenty']()).toEqual(dokumenty);
    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
    expect(fixture.componentInstance['komunikatBledu']()).toBeNull();
    const tekst = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(tekst).toContain('Decyzja administracyjna');
  });

  it('pokazuje stan "wczytywanie" odrębny od pustej listy, dopóki żądanie nie wróci', () => {
    apiStub.lista.mockReturnValue(NEVER);
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelDokumenty);
    fixture.detectChanges();

    expect(fixture.componentInstance['wczytywanie']()).toBe(true);
    const tekst = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(tekst).not.toContain('Brak dokumentów powiązanych ze sprawami tego obywatela.');
    expect(fixture.nativeElement.querySelector('#obywatel-dokumenty-wczytywanie')).toBeTruthy();
  });

  it('pokazuje faktycznie pustą listę ("brak dokumentów"), gdy wczytywanie się zakończyło bez błędu', () => {
    apiStub.lista.mockReturnValue(of([]));
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelDokumenty);
    fixture.detectChanges();

    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
    expect(fixture.componentInstance['komunikatBledu']()).toBeNull();
    const tekst = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(tekst).toContain('Brak dokumentów powiązanych ze sprawami tego obywatela.');
  });

  it('pokazuje zmapowany komunikat błędu HTTP, bez nadpisywania przez pustą listę', () => {
    apiStub.lista.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelDokumenty);
    fixture.detectChanges();

    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
    expect(fixture.componentInstance['komunikatBledu']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
    expect(fixture.nativeElement.querySelector('#obywatel-dokumenty-blad')).toBeTruthy();
  });
});
