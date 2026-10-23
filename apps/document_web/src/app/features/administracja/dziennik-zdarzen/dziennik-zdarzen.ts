import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { ButtonModule } from 'primeng/button';
import { MOCK_DZIENNIK_ZDARZEN } from '../mock/admin-mock-data';
import type { TypOperacjiAudytu } from '@electronic-document-workflow/document-shared-dto';

@Component({
  selector: 'app-dziennik-zdarzen',
  standalone: true,
  imports: [FormsModule, TableModule, SelectModule, DatePickerModule, ButtonModule],
  templateUrl: './dziennik-zdarzen.html',
  styleUrl: './dziennik-zdarzen.css',
})
export class DziennikZdarzen {
  readonly wpisy = MOCK_DZIENNIK_ZDARZEN;

  readonly typyOperacji: TypOperacjiAudytu[] = Array.from(
    new Set(this.wpisy.map((w) => w.typOperacji)),
  );

  readonly filtrTypu = signal<TypOperacjiAudytu | null>(null);
  readonly filtrZakresDat = signal<Date[] | null>(null);

  readonly wpisyPrzefiltrowane = computed(() => {
    const typ = this.filtrTypu();
    const zakres = this.filtrZakresDat();
    return this.wpisy.filter((wpis) => {
      if (typ && wpis.typOperacji !== typ) {
        return false;
      }
      if (zakres && zakres[0]) {
        const data = new Date(wpis.dataCzas.replace(' ', 'T'));
        const od = zakres[0];
        const do_ = zakres[1] ?? zakres[0];
        if (data < startDnia(od) || data > koniecDnia(do_)) {
          return false;
        }
      }
      return true;
    });
  });

  wyczyscFiltry(): void {
    this.filtrTypu.set(null);
    this.filtrZakresDat.set(null);
  }
}

function startDnia(data: Date): Date {
  const kopia = new Date(data);
  kopia.setHours(0, 0, 0, 0);
  return kopia;
}

function koniecDnia(data: Date): Date {
  const kopia = new Date(data);
  kopia.setHours(23, 59, 59, 999);
  return kopia;
}
