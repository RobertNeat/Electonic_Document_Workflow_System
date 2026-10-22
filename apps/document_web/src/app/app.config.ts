import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localePl from '@angular/common/locales/pl';
import { provideRouter } from '@angular/router';
import { providePrimeNG } from 'primeng/config';
import { routes } from './app.routes';
import { AppThemePreset } from './core/theme/app-theme.preset';
import { PRIMENG_TLUMACZENIE_PL } from './core/config/primeng-pl.translation';

registerLocaleData(localePl);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: LOCALE_ID, useValue: 'pl' },
    provideRouter(routes),
    providePrimeNG({
      theme: {
        preset: AppThemePreset,
        options: {
          darkModeSelector: '.app-dark',
        },
      },
      translation: PRIMENG_TLUMACZENIE_PL,
    }),
  ],
};
