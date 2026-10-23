import type { Sprawa } from '@electronic-document-workflow/document-shared-dto';

/**
 * Dodatkowe sprawy referenta na potrzeby Pulpitu — core/mock/mock-data.ts zawiera
 * tylko 3 sprawy (tylko jedna przypisana do Katarzyny Zielińskiej), co nie wystarcza
 * do zademonstrowania liczników i alertów terminowych. Terminy liczone względem
 * dzisiejszej daty, aby demo działało niezależnie od tego kiedy jest odpalane.
 * Uwaga: identyfikatory tych spraw nie istnieją w core/mock/mock-data.ts, więc
 * routerLink do /sprawy/:id nie wyświetli szczegółów — docelowo ta sekcja danych
 * powinna trafić do core/mock/mock-data.ts (zgłoszone w raporcie).
 */
function dataWzgledna(dni: number): string {
  const d = new Date();
  d.setDate(d.getDate() + dni);
  return d.toISOString().slice(0, 10);
}

export const PULPIT_DODATKOWE_SPRAWY: Sprawa[] = [
  {
    id: 'spr-p1',
    znakSprawy: 'WO.6220.21.2026',
    tytul: 'Wniosek o wydanie decyzji środowiskowej — budowa hali magazynowej',
    jrwa: '6220',
    definicjaObieguId: 'obieg-decyzja',
    stan: 'W toku',
    obywatelId: 'ob-3',
    wydzial: 'Wydział Ochrony Środowiska',
    referent: 'Katarzyna Zielińska',
    dataWszczecia: dataWzgledna(-30),
    terminUstawowy: dataWzgledna(-3),
    kategoriaArchiwalna: 'A',
    zamknieta: false,
  },
  {
    id: 'spr-p2',
    znakSprawy: 'WO.6220.22.2026',
    tytul: 'Wniosek o wydanie decyzji środowiskowej — stacja paliw',
    jrwa: '6220',
    definicjaObieguId: 'obieg-decyzja',
    stan: 'Do akceptacji',
    obywatelId: 'ob-2',
    wydzial: 'Wydział Ochrony Środowiska',
    referent: 'Katarzyna Zielińska',
    dataWszczecia: dataWzgledna(-14),
    terminUstawowy: dataWzgledna(4),
    kategoriaArchiwalna: 'A',
    zamknieta: false,
  },
  {
    id: 'spr-p3',
    znakSprawy: 'WO.6220.23.2026',
    tytul: 'Wniosek o wydanie decyzji środowiskowej — przebudowa drogi',
    jrwa: '6220',
    definicjaObieguId: 'obieg-decyzja',
    stan: 'W toku',
    obywatelId: 'ob-1',
    wydzial: 'Wydział Ochrony Środowiska',
    referent: 'Katarzyna Zielińska',
    dataWszczecia: dataWzgledna(-5),
    terminUstawowy: dataWzgledna(20),
    kategoriaArchiwalna: 'A',
    zamknieta: false,
  },
];
