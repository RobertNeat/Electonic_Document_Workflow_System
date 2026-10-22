import { Routes } from '@angular/router';

export const DOKUMENTY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./dokumenty-strona/dokumenty-strona').then((m) => m.DokumentyStrona),
  },
  {
    path: 'szablony',
    loadComponent: () => import('./szablony-strona/szablony-strona').then((m) => m.SzablonyStrona),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./dokument-szczegoly/dokument-szczegoly').then((m) => m.DokumentSzczegoly),
  },
];
