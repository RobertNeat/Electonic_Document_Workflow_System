// Konfiguracja produkcyjna/domyślna. Wartości wpiekane w bundle w czasie builda
// (brak Dockerfile/mechanizmu runtime-config dla document_web) - zmiana wymaga
// przebudowania aplikacji, nie tylko zmiany zmiennej środowiskowej kontenera.
export const environment = {
  keycloakIssuer: 'http://localhost:10681/realms/document-workflow',
  keycloakClientId: 'document-web',
  documentApiBaseUrl: 'http://localhost:10620', // nieużywane w tym cyklu, zarezerwowane na kolejny etap
};
