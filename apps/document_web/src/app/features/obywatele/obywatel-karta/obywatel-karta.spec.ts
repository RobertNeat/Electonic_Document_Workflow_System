import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import type { Obywatel } from '@electronic-document-workflow/document-shared-dto';
import { ObywatelKarta } from './obywatel-karta';
import { ObywateleDaneService } from '../obywatele-dane.service';
import { CitizenContextService } from '../../../core/services/citizen-context.service';

describe('ObywatelKarta', () => {
  let daneStub: { znajdz: ReturnType<typeof vi.fn> };
  let citizenContextStub: { przypnij: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    daneStub = { znajdz: vi.fn() };
    citizenContextStub = { przypnij: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ObywatelKarta],
      providers: [
        provideRouter([]),
        { provide: ObywateleDaneService, useValue: daneStub },
        { provide: CitizenContextService, useValue: citizenContextStub },
      ],
    });
  });

  it('wczytuje obywatela i ustawia kontekst/przypina obywatela przy sukcesie', () => {
    const obywatel = { id: 'ob-1', typ: 'osoba_fizyczna' } as Obywatel;
    daneStub.znajdz.mockReturnValue(of(obywatel));

    const fixture = TestBed.createComponent(ObywatelKarta);
    fixture.componentRef.setInput('id', 'ob-1');
    fixture.detectChanges();

    expect(daneStub.znajdz).toHaveBeenCalledWith('ob-1');
    expect(fixture.componentInstance['kontekst'].obywatel()).toEqual(obywatel);
    expect(fixture.componentInstance['wczytywanie']()).toBe(false);
    expect(fixture.componentInstance['nieznaleziony']()).toBe(false);
    expect(citizenContextStub.przypnij).toHaveBeenCalledWith(obywatel);
  });

  it('ustawia nieznaleziony() przy błędzie 404, bez przypinania obywatela', () => {
    daneStub.znajdz.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));

    const fixture = TestBed.createComponent(ObywatelKarta);
    fixture.componentRef.setInput('id', 'brak-takiego');
    fixture.detectChanges();

    expect(fixture.componentInstance['nieznaleziony']()).toBe(true);
    expect(fixture.componentInstance['komunikatBledu']()).toBeNull();
    expect(citizenContextStub.przypnij).not.toHaveBeenCalled();
  });

  it('ustawia komunikatBledu() przy błędzie sieci/CORS (status 0)', () => {
    daneStub.znajdz.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = TestBed.createComponent(ObywatelKarta);
    fixture.componentRef.setInput('id', 'ob-1');
    fixture.detectChanges();

    expect(fixture.componentInstance['nieznaleziony']()).toBe(false);
    expect(fixture.componentInstance['komunikatBledu']()).toBe(
      'Brak połączenia z serwerem (sieć lub CORS).',
    );
  });
});
