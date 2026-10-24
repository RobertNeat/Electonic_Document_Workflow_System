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

export interface SzczegolyPodpisu {
  podpisujacy: string;
  czasPodpisu: string;
  odciskCertyfikatu: string;
  statusOcsp: string;
  statusCrl: string;
  znacznikCzasuTsa: string;
}

export interface DokumentListElement extends Dokument {
  znakSprawy: string;
  statusPodpisuAktualnej: StatusPodpisu;
  typDokumentuNazwa: string;
}
