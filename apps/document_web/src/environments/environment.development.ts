// Konfiguracja dla `ng serve` (development) - te same wartości domyślne co compose.yml,
// wpiekane w bundle w czasie builda (patrz environment.ts).
export const environment = {
  keycloakIssuer: 'http://localhost:10681/realms/document-workflow',
  keycloakClientId: 'document-web',
  documentApiBaseUrl: 'http://localhost:10620', // nieużywane w tym cyklu, zarezerwowane na kolejny etap
};
