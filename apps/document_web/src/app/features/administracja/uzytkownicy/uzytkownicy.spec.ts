import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import type { Uzytkownik, Wydzial } from '@electronic-document-workflow/document-shared-dto';
import { Uzytkownicy } from './uzytkownicy';
import { AdministracjaApi } from '../../../core/api/administracja.api';
import { AuthService } from '../../../core/services/auth.service';

describe('Uzytkownicy', () => {
  let apiStub: {
    uzytkownicy: ReturnType<typeof vi.fn>;
    strukturaOrganizacyjna: ReturnType<typeof vi.fn>;
    utworzUzytkownika: ReturnType<typeof vi.fn>;
    aktualizujRoleUzytkownika: ReturnType<typeof vi.fn>;
  };

  const uzytkownicy: Uzytkownik[] = [
    {
      id: 'u-1',
      login: 'jan.kowalski',
      imie: 'Jan',
      nazwisko: 'Kowalski',
      email: 'jan.kowalski@urzad.gov.pl',
      role: ['referent'],
      wydzial: 'Wydział Organizacyjny',
      stanowisko: 'Referent',
    },
    {
      id: 'u-2',
      login: 'ewa.nowak',
      imie: 'Ewa',
      nazwisko: 'Nowak',
      email: 'ewa.nowak@urzad.gov.pl',
      role: ['kancelaria'],
      wydzial: 'Wydział Organizacyjny',
      stanowisko: 'Pracownik kancelarii',
    },
  ];

  const wydzialy: Wydzial[] = [
    { id: 'w-1', nazwa: 'Wydział Organizacyjny', kierownik: 'Kierownik', stanowiska: [] },
  ];

  function skonfigurujTestBed(role: ('administrator' | string)[]) {
    apiStub = {
      uzytkownicy: vi.fn().mockReturnValue(of(uzytkownicy)),
      strukturaOrganizacyjna: vi.fn().mockReturnValue(of(wydzialy)),
      utworzUzytkownika: vi.fn(),
      aktualizujRoleUzytkownika: vi.fn(),
    };
    const authStub = {
      uzytkownik: signal({
        id: 'me',
        login: 'me',
        imie: 'Me',
        nazwisko: 'Me',
        email: 'me@urzad.gov.pl',
        role,
        wydzial: '',
        stanowisko: '',
      } as Uzytkownik),
    };
    TestBed.configureTestingModule({
      imports: [Uzytkownicy],
      providers: [
        { provide: AdministracjaApi, useValue: apiStub },
        { provide: AuthService, useValue: authStub },
      ],
    });
  }

  it('dla administratora wczytuje listę użytkowników i wydziałów', () => {
    skonfigurujTestBed(['administrator']);
    const fixture = TestBed.createComponent(Uzytkownicy);
    fixture.detectChanges();

    expect(apiStub.uzytkownicy).toHaveBeenCalled();
    expect(fixture.componentInstance.uzytkownicy()).toEqual(uzytkownicy);
    expect(fixture.componentInstance.wydzialyNazwy()).toEqual(['Wydział Organizacyjny']);
    expect(fixture.componentInstance.maDostep()).toBe(true);
  });

  it('dla roli innej niż administrator nie woła API i ustawia maDostep() na false', () => {
    skonfigurujTestBed(['referent']);
    const fixture = TestBed.createComponent(Uzytkownicy);
    fixture.detectChanges();

    expect(apiStub.uzytkownicy).not.toHaveBeenCalled();
    expect(fixture.componentInstance.maDostep()).toBe(false);
    expect(fixture.componentInstance.wczytywanie()).toBe(false);
  });

  it('pokazuje komunikat błędu, gdy uzytkownicy() zawiedzie', () => {
    skonfigurujTestBed(['administrator']);
    apiStub.uzytkownicy.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = TestBed.createComponent(Uzytkownicy);
    fixture.detectChanges();

    expect(fixture.componentInstance.komunikatBledu()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
  });

  it('zapiszNowegoUzytkownika() woła utworzUzytkownika() i dopisuje wynik z API do listy', () => {
    skonfigurujTestBed(['administrator']);
    const nowy: Uzytkownik = {
      id: 'u-3',
      login: 'adam.wojcik',
      imie: 'Adam',
      nazwisko: 'Wójcik',
      email: 'adam.wojcik@urzad.gov.pl',
      role: ['referent'],
      wydzial: 'Wydział Organizacyjny',
      stanowisko: 'Referent',
    };
    apiStub.utworzUzytkownika.mockReturnValue(of(nowy));

    const fixture = TestBed.createComponent(Uzytkownicy);
    fixture.detectChanges();
    fixture.componentInstance.otworzDialogDodania();
    fixture.componentInstance.nowyUzytkownik.set({
      login: 'adam.wojcik',
      imie: 'Adam',
      nazwisko: 'Wójcik',
      email: 'adam.wojcik@urzad.gov.pl',
      role: ['referent'],
      wydzial: 'Wydział Organizacyjny',
      stanowisko: 'Referent',
    });
    fixture.componentInstance.zapiszNowegoUzytkownika();

    expect(apiStub.utworzUzytkownika).toHaveBeenCalled();
    expect(fixture.componentInstance.uzytkownicy()).toHaveLength(3);
    expect(fixture.componentInstance.uzytkownicy()[2]).toEqual(nowy);
    expect(fixture.componentInstance.widocznyDialogDodania()).toBe(false);
  });

  it('zapiszNowegoUzytkownika() pokazuje komunikat błędu (np. duplikat loginu) i nie zamyka dialogu', () => {
    skonfigurujTestBed(['administrator']);
    apiStub.utworzUzytkownika.mockReturnValue(
      throwError(
        () => new HttpErrorResponse({ status: 400, error: { komunikat: 'Login zajęty.' } }),
      ),
    );

    const fixture = TestBed.createComponent(Uzytkownicy);
    fixture.detectChanges();
    fixture.componentInstance.otworzDialogDodania();
    fixture.componentInstance.nowyUzytkownik.set({
      login: 'jan.kowalski',
      imie: 'Jan',
      nazwisko: 'Kowalski',
      email: 'jan@urzad.gov.pl',
      role: [],
      wydzial: 'Wydział Organizacyjny',
      stanowisko: 'Referent',
    });
    fixture.componentInstance.zapiszNowegoUzytkownika();

    expect(fixture.componentInstance.dodanieBlad()).toBe('Login zajęty.');
    expect(fixture.componentInstance.widocznyDialogDodania()).toBe(true);
    expect(fixture.componentInstance.uzytkownicy()).toHaveLength(2);
  });

  it('aktualizujRole() woła aktualizujRoleUzytkownika() i zastępuje wiersz odpowiedzią serwera', () => {
    skonfigurujTestBed(['administrator']);
    const zaktualizowany: Uzytkownik = { ...uzytkownicy[0], role: ['referent', 'kierownik'] };
    apiStub.aktualizujRoleUzytkownika.mockReturnValue(of(zaktualizowany));

    const fixture = TestBed.createComponent(Uzytkownicy);
    fixture.detectChanges();
    fixture.componentInstance.aktualizujRole(uzytkownicy[0], ['referent', 'kierownik']);

    expect(apiStub.aktualizujRoleUzytkownika).toHaveBeenCalledWith('u-1', [
      'referent',
      'kierownik',
    ]);
    expect(fixture.componentInstance.uzytkownicy()[0]).toEqual(zaktualizowany);
  });

  it('aktualizujRole() nie woła API i pokazuje komunikat, gdy usuwane są wszystkie role (@NotEmpty)', () => {
    skonfigurujTestBed(['administrator']);

    const fixture = TestBed.createComponent(Uzytkownicy);
    fixture.detectChanges();
    fixture.componentInstance.aktualizujRole(uzytkownicy[0], []);

    expect(apiStub.aktualizujRoleUzytkownika).not.toHaveBeenCalled();
    expect(fixture.componentInstance.komunikatBledu()).toBe(
      'Użytkownik musi mieć przypisaną co najmniej jedną rolę.',
    );
    expect(fixture.componentInstance.uzytkownicy()[0].role).toEqual(uzytkownicy[0].role);
  });

  it('zastosujAktualizacjeRolZbiorczo() woła PUT sekwencyjnie per użytkownik i czyści wybranych po sukcesie', async () => {
    skonfigurujTestBed(['administrator']);
    apiStub.aktualizujRoleUzytkownika.mockImplementation((id: string, role: string[]) =>
      of({ ...uzytkownicy.find((u) => u.id === id)!, role } as Uzytkownik),
    );

    const fixture = TestBed.createComponent(Uzytkownicy);
    fixture.detectChanges();
    fixture.componentInstance.wybrani.set(uzytkownicy);
    fixture.componentInstance.dodajRoleZbiorczo.set(['kierownik']);

    await fixture.componentInstance.zastosujAktualizacjeRolZbiorczo();

    expect(apiStub.aktualizujRoleUzytkownika).toHaveBeenCalledTimes(2);
    expect(fixture.componentInstance.wybrani()).toEqual([]);
    expect(fixture.componentInstance.widocznyDialogRol()).toBe(false);
  });

  it('zastosujAktualizacjeRolZbiorczo() zbiera częściowe niepowodzenia i zostawia tylko nieudane konta wybrane', async () => {
    skonfigurujTestBed(['administrator']);
    apiStub.aktualizujRoleUzytkownika.mockImplementation((id: string) => {
      if (id === 'u-1') {
        return of({ ...uzytkownicy[0], role: ['referent', 'kierownik'] } as Uzytkownik);
      }
      return throwError(() => new HttpErrorResponse({ status: 403 }));
    });

    const fixture = TestBed.createComponent(Uzytkownicy);
    fixture.detectChanges();
    fixture.componentInstance.otworzDialogRol();
    fixture.componentInstance.wybrani.set(uzytkownicy);
    fixture.componentInstance.dodajRoleZbiorczo.set(['kierownik']);

    await fixture.componentInstance.zastosujAktualizacjeRolZbiorczo();

    expect(fixture.componentInstance.rolZbiorczoBlad()).toContain('ewa.nowak');
    expect(fixture.componentInstance.wybrani().map((u) => u.login)).toEqual(['ewa.nowak']);
    expect(fixture.componentInstance.widocznyDialogRol()).toBe(true);
  });
});
