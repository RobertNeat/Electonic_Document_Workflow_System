export type StatusPodpisu =
  'brak' | 'poprawny' | 'niepoprawny' | 'certyfikat_wygasly' | 'certyfikat_uniewazniony';

export interface TypDokumentu {
  id: string;
  nazwa: string;
  opis: string;
}

export interface SzablonJasper {
  id: string;
  typDokumentuId: string;
  nazwa: string;
  wersja: number;
  dataWgrania: string;
  autor: string;
}

export interface WersjaDokumentu {
  id: string;
  dokumentId: string;
  wersja: number;
  podpisana: boolean;
  dataUtworzenia: string;
  autor: string;
  statusPodpisu: StatusPodpisu;
}

export interface Dokument {
  id: string;
  sprawaId: string;
  typDokumentuId: string;
  nazwa: string;
  aktualnaWersja: number;
  dataUtworzenia: string;
}

/** czasPodpisu/znacznikCzasuTsa mogą być null - sign_service zwraca literalne "brak"/"nieznany"
 * tam, gdzie oczekiwany jest czas (np. brak tokenu TSA dla podpisu BASELINE-B), document_api
 * mapuje takie niesparsowalne wartości na null. statusOcsp/statusCrl są realnym, zbiorczym
 * statusem z sign_service (zawsze "nieznany" - sign_service nie rozbija weryfikacji na osobne
 * kanały OCSP/CRL), nie fejkowym tłumaczeniem. */
export interface SzczegolyPodpisu {
  podpisujacy: string;
  czasPodpisu: string | null;
  odciskCertyfikatu: string;
  statusOcsp: string;
  statusCrl: string;
  znacznikCzasuTsa: string | null;
}

/** Metadane certyfikatu z sign_service (proxy przez document_api GET /api/certyfikaty) - do
 * wyboru w cert-pickerze przy podpisywaniu dokumentu. `podmiot` jest pełnym DN certyfikatu
 * X.509 (np. "CN=Katarzyna Zielińska, O=Urząd, C=PL"), nie loginem użytkownika. */
export interface Certyfikat {
  id: string;
  podmiot: string;
  numerSeryjny: string;
  dataWydania: string;
  dataWygasniecia: string;
  status: string;
  wydawca: string;
}

export interface DokumentListElement extends Dokument {
  znakSprawy: string;
  statusPodpisuAktualnej: StatusPodpisu;
  typDokumentuNazwa: string;
}
