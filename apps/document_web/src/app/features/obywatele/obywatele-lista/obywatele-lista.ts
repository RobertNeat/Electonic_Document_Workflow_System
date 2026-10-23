import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { obywatelWyswietlanaNazwa } from '../../../core/models';
import type { Obywatel } from '@electronic-document-workflow/document-shared-dto';
import { ObywateleDaneService } from '../obywatele-dane.service';

@Component({
  selector: 'app-obywatele-lista',
  imports: [FormsModule, TableModule, TagModule, InputTextModule, IconFieldModule, InputIconModule],
  templateUrl: './obywatele-lista.html',
  styleUrl: './obywatele-lista.css',
})
export class ObywateleLista {
  private readonly dane = inject(ObywateleDaneService);
  private readonly router = inject(Router);

  protected readonly zapytanie = signal('');
  protected readonly obywatele = computed(() => this.dane.szukaj(this.zapytanie()));

  protected readonly obywatelWyswietlanaNazwa = obywatelWyswietlanaNazwa;

  protected otworz(obywatel: Obywatel): void {
    this.router.navigate(['/obywatele', obywatel.id]);
  }
}
