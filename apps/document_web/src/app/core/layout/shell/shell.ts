import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Topbar } from '../topbar/topbar';
import { Sidebar } from '../sidebar/sidebar';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, Topbar, Sidebar],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell {
  protected readonly sidebarZwinieta = signal(false);

  protected przelaczSidebar(): void {
    this.sidebarZwinieta.update((v) => !v);
  }
}
