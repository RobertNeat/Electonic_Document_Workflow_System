package robert_neat.document_api.service;

/** Bajty pliku PDF danej wersji dokumentu (pobrane z dms_service) wraz z nazwą pliku do
 * zbudowania Content-Disposition w kontrolerze. */
public record PlikWersjiDokumentu(byte[] bajty, String nazwaPliku) {
}
