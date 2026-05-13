import { Component, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { Api } from '../../core/api';
import { Invoice, Page } from '../../core/models';
import { invoiceStatusClasses } from '../../core/format';

@Component({
  selector: 'app-invoices',
  imports: [CurrencyPipe, DatePipe, MatCardModule, MatPaginatorModule],
  template: `
    <h1 class="text-2xl font-semibold mb-6">Invoices</h1>

    <mat-card class="p-0 overflow-hidden">
      <table class="w-full text-sm">
        <thead class="bg-slate-50 text-slate-500 text-left">
          <tr>
            <th class="p-3">Invoice</th><th class="p-3">Period</th><th class="p-3">Amount</th>
            <th class="p-3">Attempts</th><th class="p-3">Status</th>
          </tr>
        </thead>
        <tbody>
          @for (invoice of page()?.content ?? []; track invoice.id) {
            <tr class="border-t">
              <td class="p-3 font-mono text-xs">{{ invoice.number }}</td>
              <td class="p-3">{{ invoice.periodStart | date: 'mediumDate' }} – {{ invoice.periodEnd | date: 'mediumDate' }}</td>
              <td class="p-3">{{ invoice.amountCents / 100 | currency: invoice.currency }}</td>
              <td class="p-3">{{ invoice.attemptCount }}</td>
              <td class="p-3">
                <span class="px-2 py-0.5 rounded-full text-xs font-medium" [class]="statusClasses(invoice.status)">
                  {{ invoice.status }}
                </span>
                @if (invoice.failureReason) {
                  <span class="text-xs text-red-600 ml-2">{{ invoice.failureReason }}</span>
                }
              </td>
            </tr>
          } @empty {
            <tr><td class="p-8 text-center text-slate-500" colspan="5">No invoices yet.</td></tr>
          }
        </tbody>
      </table>
      <mat-paginator
        [length]="page()?.page?.totalElements ?? 0"
        [pageSize]="page()?.page?.size ?? 20"
        [pageIndex]="page()?.page?.number ?? 0"
        [pageSizeOptions]="[10, 20, 50]"
        (page)="onPage($event)">
      </mat-paginator>
    </mat-card>
  `,
})
export class Invoices implements OnInit {
  private api = inject(Api);

  protected readonly page = signal<Page<Invoice> | null>(null);
  protected readonly statusClasses = invoiceStatusClasses;

  ngOnInit(): void {
    this.load(0, 20);
  }

  onPage(event: PageEvent): void {
    this.load(event.pageIndex, event.pageSize);
  }

  private load(pageIndex: number, pageSize: number): void {
    this.api.invoices(pageIndex, pageSize).subscribe(page => this.page.set(page));
  }
}
