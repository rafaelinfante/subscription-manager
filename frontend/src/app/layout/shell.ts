import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { Auth } from '../core/auth';

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule, MatIconModule, MatButtonModule,
  ],
  template: `
    <mat-toolbar color="primary" class="!sticky top-0 z-10 flex items-center gap-2">
      <mat-icon>credit_card</mat-icon>
      <span class="font-medium">Subscription Manager</span>
      <span class="flex-1"></span>
      <span class="text-sm opacity-90 hidden sm:inline">{{ auth.user()?.email }}</span>
      <button mat-button (click)="auth.logout()">
        <mat-icon>logout</mat-icon>
        Sign out
      </button>
    </mat-toolbar>

    <mat-sidenav-container class="min-h-[calc(100vh-64px)]">
      <mat-sidenav mode="side" opened class="w-60 border-r">
        <mat-nav-list>
          @for (item of navItems; track item.path) {
            @if (!item.adminOnly || auth.isAdmin()) {
              <a mat-list-item [routerLink]="item.path" routerLinkActive="bg-slate-100">
                <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
                <span matListItemTitle>{{ item.label }}</span>
              </a>
            }
          }
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content class="p-6">
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
})
export class Shell {
  protected readonly auth = inject(Auth);

  protected readonly navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: 'dashboard', adminOnly: false },
    { path: '/plans', label: 'Plans', icon: 'sell', adminOnly: false },
    { path: '/subscriptions', label: 'Subscriptions', icon: 'autorenew', adminOnly: false },
    { path: '/invoices', label: 'Invoices', icon: 'receipt_long', adminOnly: false },
    { path: '/admin', label: 'Admin', icon: 'admin_panel_settings', adminOnly: true },
  ];
}
