package robert_neat.sign_service.dto;

/**
 * Wynik weryfikacji podpisu PAdES. UWAGA - rozbieznosc statusow w projekcie: kolumna
 * `pki.wystawiony_podpis.status_weryfikacji` przyjmuje tylko 3 wartosci (poprawny/niepoprawny/
 * nieznany), ale TypeScript `StatusPodpisu` (packages/document-shared-dto) i
 * `core.wersja_dokumentu.status_podpisu` w document_api mają 4 wartosci dla podpisu istniejacego
 * (poprawny/niepoprawny/certyfikat_wygasly/certyfikat_uniewazniony - bez "brak", ten dotyczy tylko
 * braku podpisu). Ten rekord zwraca ten SZERSZY, 4-wartosciowy zestaw - OBLICZONY z polaczenia
 * kryptograficznej weryfikacji DSS (integralnosc/autentycznosc podpisu) z lokalnym sprawdzeniem
 * stanu certyfikatu (tabela `uniewaznienie` + `data_wygasniecia`), NIE z odczytu kolumny
 * `status_weryfikacji` (ta zostaje węższa, używana tylko przy zapisie nowo złożonego podpisu).
 *
 * Pola odpowiadaja kontraktowi SzczegolyPodpisu (packages/document-shared-dto/src/dokumenty.ts) +
 * pole statusu.
 */
public record WynikWeryfikacji(
        String status,
        String podpisujacy,
        String czasPodpisu,
        String odciskCertyfikatu,
        String statusOcsp,
        String statusCrl,
        String znacznikCzasuTsa
) {
}
