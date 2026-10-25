import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import type {
  DefinicjaObiegu,
  Uzytkownik,
} from '@electronic-document-workflow/document-shared-dto';
import { Obiegi } from './obiegi';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { AuthService } from '../../../core/services/auth.service';

describe('Obiegi', () => {
  let apiStub: {
    definicjeObiegow: ReturnType<typeof vi.fn>;
    edytujDefinicjeObiegu: ReturnType<typeof vi.fn>;
  };

  const definicje: DefinicjaObiegu[] = [
    {
      id: 'obieg-1',
      nazwa: 'Decyzja administracyjna',
      opis: 'Obieg standardowy',
      stany: ['Zarejestrowana', 'W toku', 'Zakończona'],
      stanPoczatkowy: 'Zarejestrowana',
      przejscia: [],
    },
  ];

  function skonfigurujTestBed(role: string[] = ['administrator']) {
    apiStub = {
      definicjeObiegow: vi.fn().mockReturnValue(of(definicje)),
      edytujDefinicjeObiegu: vi.fn(),
    };
    const authStub = {
      uzytkownik: signal({ role } as unknown as Uzytkownik),
    };
    TestBed.configureTestingModule({
      imports: [Obiegi],
      providers: [
        { provide: SprawyApi, useValue: apiStub },
        { provide: AuthService, useValue: authStub },
      ],
    });
  }

  beforeEach(() => skonfigurujTestBed());

  it('wczytuje definicje obiegów przy montowaniu', () => {
    const fixture = TestBed.createComponent(Obiegi);
    fixture.detectChanges();

    expect(apiStub.definicjeObiegow).toHaveBeenCalled();
    expect(fixture.componentInstance.definicje()).toEqual(definicje);
    expect(fixture.componentInstance.wczytywanie()).toBe(false);
  });

  it('pokazuje komunikat błędu, gdy definicjeObiegow() zawiedzie', () => {
    apiStub.definicjeObiegow.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0 })),
    );

    const fixture = TestBed.createComponent(Obiegi);
    fixture.detectChanges();

    expect(fixture.componentInstance.komunikatBledu()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
  });

  it('zapiszEdycje() woła edytujDefinicjeObiegu() i aktualizuje listę z odpowiedzią serwera', () => {
    const zaktualizowana: DefinicjaObiegu = {
      ...definicje[0],
      nazwa: 'Nowa nazwa',
      opis: 'Nowy opis',
    };
    apiStub.edytujDefinicjeObiegu.mockReturnValue(of(zaktualizowana));

    const fixture = TestBed.createComponent(Obiegi);
    fixture.detectChanges();
    fixture.componentInstance.otworzEdycje(definicje[0]);
    fixture.componentInstance.formularzEdycji.set({ nazwa: 'Nowa nazwa', opis: 'Nowy opis' });
    fixture.componentInstance.zapiszEdycje();

    expect(apiStub.edytujDefinicjeObiegu).toHaveBeenCalledWith('obieg-1', {
      nazwa: 'Nowa nazwa',
      opis: 'Nowy opis',
    });
    expect(fixture.componentInstance.definicje()[0]).toEqual(zaktualizowana);
    expect(fixture.componentInstance.widocznaEdycja()).toBe(false);
  });

  it('zapiszEdycje() pokazuje komunikat błędu i nie zamyka dialogu, gdy zapis zawiedzie', () => {
    apiStub.edytujDefinicjeObiegu.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403 })),
    );

    const fixture = TestBed.createComponent(Obiegi);
    fixture.detectChanges();
    fixture.componentInstance.otworzEdycje(definicje[0]);
    fixture.componentInstance.zapiszEdycje();

    expect(fixture.componentInstance.edycjaBlad()).toBe(
      'Brak uprawnień do wykonania tej operacji.',
    );
    expect(fixture.componentInstance.widocznaEdycja()).toBe(true);
    expect(fixture.componentInstance.definicje()[0]).toEqual(definicje[0]);
  });

  it('czyAdministrator() jest false dla roli innej niż administrator', () => {
    skonfigurujTestBed(['referent']);
    const fixture = TestBed.createComponent(Obiegi);
    fixture.detectChanges();

    expect(fixture.componentInstance.czyAdministrator()).toBe(false);
  });
});
