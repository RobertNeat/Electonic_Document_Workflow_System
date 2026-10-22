import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { MessageModule } from 'primeng/message';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink, ButtonModule, InputTextModule, PasswordModule, MessageModule],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  private readonly router = inject(Router);

  protected readonly imie = signal('');
  protected readonly nazwisko = signal('');
  protected readonly email = signal('');
  protected readonly haslo = signal('');
  protected readonly komunikat = signal<string | null>(null);

  protected zarejestruj(): void {
    if (!this.imie() || !this.nazwisko() || !this.email() || !this.haslo()) {
      this.komunikat.set('Wypełnij wszystkie pola.');
      return;
    }
    this.komunikat.set('Konto zostało utworzone. Możesz się zalogować.');
    setTimeout(() => this.router.navigate(['/logowanie']), 1200);
  }
}
