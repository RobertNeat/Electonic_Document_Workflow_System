import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom, of, throwError } from 'rxjs';
import type { Obywatel } from '@electronic-document-workflow/document-shared-dto';
import { ObywateleDaneService } from './obywatele-dane.service';
import { ObywateleApi } from '../../core/api/obywatele.api';

describe('ObywateleDaneService', () => {
  let apiStub: {
    szukaj: ReturnType<typeof vi.fn>;
    getById: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    apiStub = { szukaj: vi.fn(), getById: vi.fn() };
    TestBed.configureTestingModule({
      providers: [{ provide: ObywateleApi, useValue: apiStub }],
    });
  });

  it('szukaj() delegates to ObywateleApi.szukaj() and returns the result', async () => {
    const wynik: Obywatel[] = [{ id: 'ob-1' } as Obywatel];
    apiStub.szukaj.mockReturnValue(of(wynik));

    const service = TestBed.inject(ObywateleDaneService);
    const obywatele = await firstValueFrom(service.szukaj('Kowalski'));

    expect(apiStub.szukaj).toHaveBeenCalledWith('Kowalski');
    expect(obywatele).toEqual(wynik);
  });

  it('znajdz() delegates to ObywateleApi.getById() and returns the result', async () => {
    const obywatel = { id: 'ob-1' } as Obywatel;
    apiStub.getById.mockReturnValue(of(obywatel));

    const service = TestBed.inject(ObywateleDaneService);
    const wynik = await firstValueFrom(service.znajdz('ob-1'));

    expect(apiStub.getById).toHaveBeenCalledWith('ob-1');
    expect(wynik).toEqual(obywatel);
  });

  it('znajdz() propaguje błąd (w tym 404) do wołającego bez tłumienia', async () => {
    const err = new HttpErrorResponse({ status: 404 });
    apiStub.getById.mockReturnValue(throwError(() => err));

    const service = TestBed.inject(ObywateleDaneService);
    await expect(firstValueFrom(service.znajdz('nieistniejacy'))).rejects.toBe(err);
  });
});
