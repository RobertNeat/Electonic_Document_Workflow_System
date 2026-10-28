import { bootstrapApplication } from '@angular/platform-browser';
import { environment } from './environments/environment';

/**
 * /config.json jest wygenerowany w kontenerze z KEYCLOAK_PUBLIC_URL/DOCUMENT_API_PUBLIC_URL
 * (patrz nginx/docker-entrypoint.d) - adres publiczny Keycloaka/document-api zależy od
 * wdrożenia (LAN serwera), więc nie może być wpieczony w bundle w czasie builda obrazu.
 * Mutacja `environment` tutaj, przed importem app.config/app, pozwala pozostałym modułom
 * (w tym kilkudziesięciu serwisom API czytającym environment.documentApiBaseUrl jako pole
 * klasy) dalej odwoływać się do statycznego `environment` bez przerabiania ich na DI token.
 */
async function loadRuntimeConfig(): Promise<void> {
  try {
    const response = await fetch('/config.json', { cache: 'no-store' });
    if (!response.ok) {
      throw new Error(`/config.json -> HTTP ${response.status}`);
    }
    Object.assign(environment, await response.json());
  } catch (err) {
    console.error('Nie udało się wczytać /config.json, używam wartości domyślnych', err);
  }
}

loadRuntimeConfig()
  .then(() => Promise.all([import('./app/app'), import('./app/app.config')]))
  .then(([{ App }, { appConfig }]) => bootstrapApplication(App, appConfig))
  .catch((err) => console.error(err));
