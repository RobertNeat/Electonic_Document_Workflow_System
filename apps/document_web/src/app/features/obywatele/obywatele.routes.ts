import { Routes } from '@angular/router';

export const OBYWATELE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./obywatele-lista/obywatele-lista').then((m) => m.ObywateleLista),
  },
  {
    path: ':id',
    loadComponent: () => import('./obywatel-karta/obywatel-karta').then((m) => m.ObywatelKarta),
    children: [
      {
        path: '',
        loadComponent: () => import('./obywatel-dane/obywatel-dane').then((m) => m.ObywatelDane),
      },
      {
        path: 'sprawy',
        loadComponent: () =>
          import('./obywatel-sprawy/obywatel-sprawy').then((m) => m.ObywatelSprawy),
      },
      {
        path: 'dokumenty',
        loadComponent: () =>
          import('./obywatel-dokumenty/obywatel-dokumenty').then((m) => m.ObywatelDokumenty),
      },
      {
        path: 'korespondencja',
        loadComponent: () =>
          import('./obywatel-korespondencja/obywatel-korespondencja').then(
            (m) => m.ObywatelKorespondencja,
          ),
      },
      {
        path: 'rozrachunki',
        loadComponent: () =>
          import('./obywatel-rozrachunki/obywatel-rozrachunki').then((m) => m.ObywatelRozrachunki),
      },
    ],
  },
];
