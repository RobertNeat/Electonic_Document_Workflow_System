import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { Shell } from './core/layout/shell/shell';

export const routes: Routes = [
  {
    path: 'logowanie',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'rejestracja',
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'pulpit' },
      {
        path: 'pulpit',
        loadChildren: () => import('./features/pulpit/pulpit.routes').then((m) => m.PULPIT_ROUTES),
      },
      {
        path: 'sprawy',
        loadChildren: () => import('./features/sprawy/sprawy.routes').then((m) => m.SPRAWY_ROUTES),
      },
      {
        path: 'obywatele',
        loadChildren: () =>
          import('./features/obywatele/obywatele.routes').then((m) => m.OBYWATELE_ROUTES),
      },
      {
        path: 'dokumenty',
        loadChildren: () =>
          import('./features/dokumenty/dokumenty.routes').then((m) => m.DOKUMENTY_ROUTES),
      },
      {
        path: 'korespondencja',
        loadChildren: () =>
          import('./features/korespondencja/korespondencja.routes').then(
            (m) => m.KORESPONDENCJA_ROUTES,
          ),
      },
      {
        path: 'platnosci',
        loadChildren: () =>
          import('./features/platnosci/platnosci.routes').then((m) => m.PLATNOSCI_ROUTES),
      },
      {
        path: 'wymiana',
        loadChildren: () =>
          import('./features/wymiana/wymiana.routes').then((m) => m.WYMIANA_ROUTES),
      },
      {
        path: 'administracja',
        loadChildren: () =>
          import('./features/administracja/administracja.routes').then(
            (m) => m.ADMINISTRACJA_ROUTES,
          ),
      },
    ],
  },
  { path: '**', redirectTo: 'pulpit' },
];
