import { Routes } from '@angular/router';

export const PULPIT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pulpit-strona/pulpit-strona').then((m) => m.PulpitStrona),
  },
];
