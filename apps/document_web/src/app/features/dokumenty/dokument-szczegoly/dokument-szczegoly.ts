import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { TimelineModule } from 'primeng/timeline';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import {
  MOCK_DOKUMENTY,
  MOCK_TYPY_DOKUMENTOW,
  MOCK_WERSJE_DOKUMENTOW,
  MOCK_SPRAWY,
} from '../../../core/mock/mock-data';
import { StatusPodpisu, WersjaDokumentu } from '../../../core/models';

interface SzczegolyPodpisu {
  podpisujacy: string;
  czasPodpisu: string;
  odciskCertyfikatu: string;
  statusOcsp: string;
  statusCrl: string;
  znacznikCzasuTsa: string;
}

@Component({
  selector: 'app-dokument-szczegoly',
  imports: [RouterLink, TimelineModule, TagModule, ButtonModule, DialogModule],
  templateUrl: './dokument-szczegoly.html',
  styleUrl: './dokument-szczegoly.css',
})
export class DokumentSzczegoly {
  private readonly route = inject(ActivatedRoute);

  private readonly params = toSignal(this.route.paramMap, {
    initialValue: this.route.snapshot.paramMap,
  });

  protected readonly dokumentId = computed(() => this.params().get('id') ?? '');

  protected readonly dokument = computed(() =>
    MOCK_DOKUMENTY.find((d) => d.id === this.dokumentId()),
  );

  protected readonly typDokumentu = computed(() => {
    const dokument = this.dokument();
    return dokument
      ? MOCK_TYPY_DOKUMENTOW.find((t) => t.id === dokument.typDokumentuId)
      : undefined;
  });

  protected readonly sprawa = computed(() => {
    const dokument = this.dokument();
    return dokument ? MOCK_SPRAWY.find((s) => s.id === dokument.sprawaId) : undefined;
  });

  // lokalny sygnał wersji — pozwala mockowo "podpisać" wersję bez backendu
  protected readonly wersje = signal<WersjaDokumentu[]>(
    MOCK_WERSJE_DOKUMENTOW.filter((w) => w.dokumentId === this.dokumentId()).sort(
      (a, b) => b.wersja - a.wersja,
    ),
  );

  protected readonly podgladWidoczny = signal(false);
  protected readonly podgladWersja = signal<WersjaDokumentu | null>(null);

  protected readonly podpisWidoczny = signal(false);
  protected readonly podpisSzczegoly = signal<SzczegolyPodpisu | null>(null);

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

  protected podgladPdf(wersja: WersjaDokumentu): void {
    this.podgladWersja.set(wersja);
    this.podgladWidoczny.set(true);
  }

  protected pobierz(wersja: WersjaDokumentu): void {
    // mock — brak realnego pliku do pobrania, backend dms_service dostarczy binaria
    alert(`Pobieranie wersji ${wersja.wersja} — funkcja zostanie podłączona do dms_service.`);
  }

  protected podpisz(wersja: WersjaDokumentu): void {
    // mockowy proces podpisu — realny podpis PAdES realizuje sign_service
    this.wersje.update((lista) =>
      lista.map((w) =>
        w.id === wersja.id
          ? { ...w, podpisana: true, statusPodpisu: 'poprawny' as StatusPodpisu }
          : w,
      ),
    );
  }

  protected szczegolyPodpisu(wersja: WersjaDokumentu): void {
    this.podpisSzczegoly.set({
      podpisujacy: `${wersja.autor} (CN=${wersja.autor.replace(' ', '.')}, O=Urząd Miasta)`,
      czasPodpisu: `${wersja.dataUtworzenia}T12:34:56Z`,
      odciskCertyfikatu: 'A1:3F:9B:22:E4:5C:7D:11:88:0A:CC:3E:6F:2B:17:D9:4A:5E:81:BC',
      statusOcsp:
        wersja.statusPodpisu === 'poprawny'
          ? 'Aktywny (potwierdzony online)'
          : 'Odpowiedź negatywna',
      statusCrl:
        wersja.statusPodpisu === 'certyfikat_uniewazniony'
          ? 'Certyfikat na liście CRL'
          : 'Brak na liście CRL',
      znacznikCzasuTsa: `${wersja.dataUtworzenia}T12:34:57Z (TSA: Urząd Certyfikacji Krajowej)`,
    });
    this.podpisWidoczny.set(true);
  }
}
