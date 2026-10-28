// Wartości domyślne/fallback. W kontenerze main.ts nadpisuje je przed bootstrapem danymi
// z /config.json (wygenerowanego przy starcie z KEYCLOAK_PUBLIC_URL/DOCUMENT_API_PUBLIC_URL) -
// zmiana adresu publicznego na danym wdrożeniu nie wymaga przebudowania obrazu.
export const environment = {
  keycloakIssuer: 'http://localhost:10681/realms/document-workflow',
  keycloakClientId: 'document-web',
  documentApiBaseUrl: 'http://localhost:10620', // baza URL dla SprawyApi (resourceServer.allowedUrls)
};
