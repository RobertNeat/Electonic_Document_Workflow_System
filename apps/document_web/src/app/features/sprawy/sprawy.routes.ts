import { Routes } from '@angular/router';

export const SPRAWY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./sprawy-strona/sprawy-strona').then((m) => m.SprawyStrona),
  },
  {
    // Musi być zadeklarowana przed ':id', inaczej 'nowa' zostanie zinterpretowane jako identyfikator sprawy.
    path: 'nowa',
    loadComponent: () => import('./sprawa-nowa/sprawa-nowa').then((m) => m.SprawaNowa),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./sprawa-szczegoly/sprawa-szczegoly').then((m) => m.SprawaSzczegoly),
  },
];
