import { Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { FormsModule } from '@angular/forms';
import {
  MOCK_DOKUMENTY,
  MOCK_TYPY_DOKUMENTOW,
  MOCK_WERSJE_DOKUMENTOW,
  MOCK_SPRAWY,
} from '../../../core/mock/mock-data';
import { Dokument, StatusPodpisu } from '../../../core/models';

interface WierszDokumentu {
  dokument: Dokument;
  typNazwa: string;
  znakSprawy: string;
  sprawaId: string;
  statusPodpisu: StatusPodpisu;
}

@Component({
  selector: 'app-dokumenty-strona',
  imports: [RouterLink, TableModule, TagModule, SelectModule, ButtonModule, FormsModule],
  templateUrl: './dokumenty-strona.html',
  styleUrl: './dokumenty-strona.css',
})
export class DokumentyStrona {
  private readonly router = inject(Router);

  // withComponentInputBinding() wiąże query param po nazwie identycznie jak path param.
  readonly sprawaId = input<string>();

  protected readonly sprawaIdFiltr = computed(() => this.sprawaId() ?? null);

  protected readonly typyDokumentow = MOCK_TYPY_DOKUMENTOW;
  protected readonly typFiltr = signal<string | null>(null);

  private readonly wiersze = computed<WierszDokumentu[]>(() =>
    MOCK_DOKUMENTY.map((dokument) => {
      const typ = MOCK_TYPY_DOKUMENTOW.find((t) => t.id === dokument.typDokumentuId);
      const sprawa = MOCK_SPRAWY.find((s) => s.id === dokument.sprawaId);
      const wersje = MOCK_WERSJE_DOKUMENTOW.filter((w) => w.dokumentId === dokument.id);
      const najnowsza = wersje.find((w) => w.wersja === dokument.aktualnaWersja);
      return {
        dokument,
        typNazwa: typ?.nazwa ?? '—',
        znakSprawy: sprawa?.znakSprawy ?? '—',
        sprawaId: dokument.sprawaId,
        statusPodpisu: najnowsza?.statusPodpisu ?? 'brak',
      };
    }),
  );

  protected readonly wierszeFiltrowane = computed(() => {
    const sprawaId = this.sprawaIdFiltr();
    const typId = this.typFiltr();
    return this.wiersze().filter((w) => {
      if (sprawaId && w.sprawaId !== sprawaId) return false;
      if (typId && w.dokument.typDokumentuId !== typId) return false;
      return true;
    });
  });

  protected etykietaStatusu(status: StatusPodpisu): string {
    switch (status) {
      case 'poprawny':
        return 'Poprawny';
      case 'niepoprawny':
        return 'Niepoprawny';
      case 'certyfikat_wygasly':
        return 'Certyfikat wygasły';
      case 'certyfikat_uniewazniony':
        return 'Certyfikat unieważniony';
      default:
        return 'Brak (wersja robocza)';
    }
  }

  protected kolorStatusu(status: StatusPodpisu): 'success' | 'danger' | 'secondary' {
    if (status === 'poprawny') return 'success';
    if (status === 'brak') return 'secondary';
    return 'danger';
  }

  protected wyczyscFiltrSprawy(): void {
    this.router.navigate([], { queryParams: { sprawaId: null }, queryParamsHandling: 'merge' });
  }

  protected otworzDokument(dokumentId: string): void {
    this.router.navigate(['/dokumenty', dokumentId]);
  }
}
