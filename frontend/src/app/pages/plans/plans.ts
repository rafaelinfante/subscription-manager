import { Component, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, LowerCasePipe } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Api } from '../../core/api';
import { Plan } from '../../core/models';
import { SubscribeDialog, SubscribeResult } from './subscribe-dialog';

@Component({
  selector: 'app-plans',
  imports: [CurrencyPipe, LowerCasePipe, MatCardModule, MatButtonModule],
  template: `
    <h1 class="text-2xl font-semibold mb-1">Plans</h1>
    <p class="text-slate-500 mb-6">Pick a plan and subscribe. Use the declining test card to see dunning in action.</p>

    <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      @for (plan of plans(); track plan.id) {
        <mat-card class="p-6 flex flex-col">
          <h2 class="text-lg font-semibold">{{ plan.name }}</h2>
          <p class="text-slate-500 text-sm mb-4 flex-1">{{ plan.description }}</p>
          <p class="text-3xl font-semibold">
            {{ plan.amountCents / 100 | currency: plan.currency }}
            <span class="text-base font-normal text-slate-500">/ {{ plan.billingInterval | lowercase }}</span>
          </p>
          <button mat-flat-button color="primary" class="mt-4" (click)="subscribe(plan)">Subscribe</button>
        </mat-card>
      }
    </div>
  `,
})
export class Plans implements OnInit {
  private api = inject(Api);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);

  protected readonly plans = signal<Plan[]>([]);

  ngOnInit(): void {
    this.api.plans().subscribe(plans => this.plans.set(plans));
  }

  subscribe(plan: Plan): void {
    this.dialog.open(SubscribeDialog, { data: { plan }, width: '420px' })
      .afterClosed()
      .subscribe((result: SubscribeResult | undefined) => {
        if (!result) {
          return;
        }
        this.api.subscribe({ planId: plan.id, ...result }).subscribe({
          next: () => {
            this.snackBar.open(`Subscribed to ${plan.name}`, 'View', { duration: 4000 })
              .onAction().subscribe(() => this.router.navigateByUrl('/subscriptions'));
          },
          error: err => this.snackBar.open(err.error?.detail ?? 'Could not subscribe', 'Dismiss', { duration: 4000 }),
        });
      });
  }
}
