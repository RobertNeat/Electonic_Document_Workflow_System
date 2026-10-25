import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import type { Wydzial } from '@electronic-document-workflow/document-shared-dto';
import { Struktura } from './struktura';
import { AdministracjaApi } from '../../../core/api/administracja.api';

describe('Struktura', () => {
  let apiStub: { strukturaOrganizacyjna: ReturnType<typeof vi.fn> };

  const wydzialy: Wydzial[] = [
    {
      id: 'w-1',
      nazwa: 'Wydział Organizacyjny',
      kierownik: 'Piotr Lewandowski',
      stanowiska: [{ id: 'w-1-s1', nazwa: 'Kierownik wydziału' }],
    },
  ];

  beforeEach(() => {
    apiStub = { strukturaOrganizacyjna: vi.fn().mockReturnValue(of(wydzialy)) };
    TestBed.configureTestingModule({
      imports: [Struktura],
      providers: [{ provide: AdministracjaApi, useValue: apiStub }],
    });
  });

  it('wczytuje strukturę organizacyjną i buduje węzły drzewa', () => {
    const fixture = TestBed.createComponent(Struktura);
    fixture.detectChanges();

    expect(apiStub.strukturaOrganizacyjna).toHaveBeenCalled();
    const wezly = fixture.componentInstance.wezly();
    expect(wezly).toHaveLength(1);
    expect(wezly[0]).toMatchObject({
      key: 'w-1',
      label: 'Wydział Organizacyjny',
      icon: 'pi pi-building',
      data: { kierownik: 'Piotr Lewandowski' },
      expanded: true,
    });
    expect(wezly[0].children).toHaveLength(1);
    expect(wezly[0].children![0]).toMatchObject({
      key: 'w-1-s1',
      label: 'Kierownik wydziału',
      icon: 'pi pi-user',
    });
    expect(fixture.componentInstance.wczytywanie()).toBe(false);
  });

  it('pokazuje komunikat błędu, gdy strukturaOrganizacyjna() zawiedzie', () => {
    apiStub.strukturaOrganizacyjna.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0 })),
    );

    const fixture = TestBed.createComponent(Struktura);
    fixture.detectChanges();

    expect(fixture.componentInstance.komunikatBledu()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
    expect(fixture.componentInstance.wezly()).toEqual([]);
  });
});
