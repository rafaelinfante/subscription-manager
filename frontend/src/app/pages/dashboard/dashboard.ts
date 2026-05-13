import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Api } from '../../core/api';
import { Auth } from '../../core/auth';
import { Invoice, Subscription } from '../../core/models';
import { invoiceStatusClasses } from '../../core/format';

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, DatePipe, RouterLink, MatCardModule, MatButtonModule, MatIconModule],
  template: `
    <h1 class="text-2xl font-semibold mb-6">Hi {{ auth.user()?.name ?? 'there' }}</h1>

    <div class="grid gap-4 sm:grid-cols-3 mb-8">
      <mat-card class="p-5">
        <p class="text-sm text-slate-500">Active subscriptions</p>
        <p class="text-3xl font-semibold mt-1">{{ activeCount() }}</p>
      </mat-card>
      <mat-card class="p-5">
        <p class="text-sm text-slate-500">Monthly spend</p>
        <p class="text-3xl font-semibold mt-1">{{ monthlySpendCents() / 100 | currency: 'USD' }}</p>
      </mat-card>
      <mat-card class="p-5">
        <p class="text-sm text-slate-500">Next billing</p>
        <p class="text-3xl font-semibold mt-1">{{ nextBilling() ? (nextBilling() | date: 'mediumDate') : '—' }}</p>
      </mat-card>
    </div>

    <div class="flex items-center justify-between mb-3">
      <h2 class="text-lg font-medium">Recent invoices</h2>
      <a mat-button routerLink="/invoices">View all</a>
    </div>

    <mat-card class="p-0 overflow-hidden">
      @if (invoices().length) {
        <table class="w-full text-sm">
          <thead class="bg-slate-50 text-slate-500 text-left">
            <tr>
              <th class="p-3">Invoice</th><th class="p-3">Issued</th>
              <th class="p-3">Amount</th><th class="p-3">Status</th>
            </tr>
          </thead>
          <tbody>
            @for (invoice of invoices(); track invoice.id) {
              <tr class="border-t">
                <td class="p-3 font-mono text-xs">{{ invoice.number }}</td>
                <td class="p-3">{{ invoice.issuedAt | date: 'mediumDate' }}</td>
                <td class="p-3">{{ invoice.amountCents / 100 | currency: invoice.currency }}</td>
                <td class="p-3">
                  <span class="px-2 py-0.5 rounded-full text-xs font-medium" [class]="statusClasses(invoice.status)">
                    {{ invoice.status }}
                  </span>
                </td>
              </tr>
            }
          </tbody>
        </table>
      } @else {
        <div class="p-8 text-center text-slate-500">
          <p class="mb-3">No invoices yet.</p>
          <a mat-flat-button color="primary" routerLink="/plans">Browse plans</a>
        </div>
      }
    </mat-card>
  `,
})
export class Dashboard implements OnInit {
  private api = inject(Api);
  protected readonly auth = inject(Auth);

  protected readonly subscriptions = signal<Subscription[]>([]);
  protected readonly invoices = signal<Invoice[]>([]);

  protected readonly statusClasses = invoiceStatusClasses;

  protected readonly activeCount = computed(() =>
    this.subscriptions().filter(s => s.status === 'ACTIVE' || s.status === 'TRIALING').length);

  protected readonly monthlySpendCents = computed(() =>
    this.subscriptions()
      .filter(s => s.status === 'ACTIVE' || s.status === 'TRIALING')
      .reduce((total, s) => total + (s.plan.billingInterval === 'YEAR' ? Math.round(s.plan.amountCents / 12) : s.plan.amountCents), 0));

  protected readonly nextBilling = computed(() => {
    const dates = this.subscriptions()
      .filter(s => s.status === 'ACTIVE' || s.status === 'TRIALING')
      .map(s => s.nextBillingDate)
      .sort();
    return dates[0] ?? null;
  });

  ngOnInit(): void {
    this.api.subscriptions(0, 100).subscribe(page => this.subscriptions.set(page.content));
    this.api.invoices(0, 5).subscribe(page => this.invoices.set(page.content));
  }
}
