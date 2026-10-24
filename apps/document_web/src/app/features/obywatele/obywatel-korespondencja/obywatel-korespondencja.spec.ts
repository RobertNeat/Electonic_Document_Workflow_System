import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { NEVER, of, throwError } from 'rxjs';
import type { Obywatel, Przesylka } from '@electronic-document-workflow/document-shared-dto';
import { ObywatelKorespondencja } from './obywatel-korespondencja';
import { PrzesylkiApi } from '../../../core/api/przesylki.api';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

describe('ObywatelKorespondencja', () => {
  let apiStub: { szukaj: ReturnType<typeof vi.fn> };
  let kontekst: KartaObywatelaKontekst;

  const obywatel = { id: 'ob-1', typ: 'osoba_fizyczna' } as Obywatel;

  beforeEach(() => {
    apiStub = { szukaj: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ObywatelKorespondencja],
      providers: [KartaObywatelaKontekst, { provide: PrzesylkiApi, useValue: apiStub }],
    });
    kontekst = TestBed.inject(KartaObywatelaKontekst);
  });

  it('wczytuje korespondencję obywatela przez PrzesylkiApi.szukaj({ obywatelId })', () => {
    const przesylki: Przesylka[] = [
      { id: 'p-1', numerReferencyjny: 'ADE/2026/00123' } as Przesylka,
    ];
    apiStub.szukaj.mockReturnValue(of(przesylki));
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelKorespondencja);
    fixture.detectChanges();

    expect(apiStub.szukaj).toHaveBeenCalledWith({ obywatelId: 'ob-1' });
    expect(fixture.componentInstance['przesylki']()).toEqual(przesylki);
    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
    expect(fixture.componentInstance['komunikatBledu']()).toBeNull();
  });

  it('pokazuje stan "wczytywanie" odrębny od pustej listy, dopóki żądanie nie wróci', () => {
    apiStub.szukaj.mockReturnValue(NEVER);
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelKorespondencja);
    fixture.detectChanges();

    expect(fixture.componentInstance['wczytywanie']()).toBe(true);
    const tekst = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(tekst).not.toContain('Brak korespondencji powiązanej z tym obywatelem.');
    expect(
      fixture.nativeElement.querySelector('#obywatel-korespondencja-wczytywanie'),
    ).toBeTruthy();
  });

  it('pokazuje faktycznie pustą listę ("brak korespondencji"), gdy wczytywanie się zakończyło bez błędu', () => {
    apiStub.szukaj.mockReturnValue(of([]));
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelKorespondencja);
    fixture.detectChanges();

    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
    expect(fixture.componentInstance['komunikatBledu']()).toBeNull();
    const tekst = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(tekst).toContain('Brak korespondencji powiązanej z tym obywatelem.');
  });

  it('pokazuje zmapowany komunikat błędu HTTP, bez nadpisywania przez pustą listę', () => {
    apiStub.szukaj.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    kontekst.ustaw(obywatel);

    const fixture = TestBed.createComponent(ObywatelKorespondencja);
    fixture.detectChanges();

    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
    expect(fixture.componentInstance['komunikatBledu']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
    expect(fixture.nativeElement.querySelector('#obywatel-korespondencja-blad')).toBeTruthy();
  });
});
