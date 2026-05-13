import { Component, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Api } from '../../core/api';
import { Plan, Subscription } from '../../core/models';
import { subscriptionStatusClasses } from '../../core/format';
import { CreatePlanDialog } from './create-plan-dialog';

@Component({
  selector: 'app-admin',
  imports: [CurrencyPipe, MatCardModule, MatButtonModule, MatIconModule],
  template: `
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-semibold">Admin</h1>
      <button mat-flat-button color="primary" (click)="runBilling()">
        <mat-icon>play_arrow</mat-icon> Run billing now
      </button>
    </div>

    <div class="flex items-center justify-between mb-3">
      <h2 class="text-lg font-medium">Plans</h2>
      <button mat-stroked-button (click)="newPlan()"><mat-icon>add</mat-icon> New plan</button>
    </div>
    <mat-card class="p-0 overflow-hidden mb-8">
      <table class="w-full text-sm">
        <thead class="bg-slate-50 text-slate-500 text-left">
          <tr><th class="p-3">Code</th><th class="p-3">Name</th><th class="p-3">Price</th><th class="p-3">Active</th><th class="p-3"></th></tr>
        </thead>
        <tbody>
          @for (plan of plans(); track plan.id) {
            <tr class="border-t">
              <td class="p-3 font-mono text-xs">{{ plan.code }}</td>
              <td class="p-3">{{ plan.name }}</td>
              <td class="p-3">{{ plan.amountCents / 100 | currency: plan.currency }} / {{ plan.billingInterval.toLowerCase() }}</td>
              <td class="p-3">{{ plan.active ? 'Yes' : 'No' }}</td>
              <td class="p-3 text-right">
                <button mat-button (click)="toggle(plan)">{{ plan.active ? 'Deactivate' : 'Activate' }}</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </mat-card>

    <h2 class="text-lg font-medium mb-3">All subscriptions</h2>
    <mat-card class="p-0 overflow-hidden">
      <table class="w-full text-sm">
        <thead class="bg-slate-50 text-slate-500 text-left">
          <tr><th class="p-3">#</th><th class="p-3">Plan</th><th class="p-3">Status</th><th class="p-3">Next billing</th></tr>
        </thead>
        <tbody>
          @for (sub of subscriptions(); track sub.id) {
            <tr class="border-t">
              <td class="p-3">{{ sub.id }}</td>
              <td class="p-3">{{ sub.plan.name }}</td>
              <td class="p-3">
                <span class="px-2 py-0.5 rounded-full text-xs font-medium" [class]="statusClasses(sub.status)">{{ sub.status }}</span>
              </td>
              <td class="p-3">{{ sub.nextBillingDate }}</td>
            </tr>
          } @empty {
            <tr><td class="p-8 text-center text-slate-500" colspan="4">No subscriptions.</td></tr>
          }
        </tbody>
      </table>
    </mat-card>
  `,
})
export class Admin implements OnInit {
  private api = inject(Api);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  protected readonly plans = signal<Plan[]>([]);
  protected readonly subscriptions = signal<Subscription[]>([]);
  protected readonly statusClasses = subscriptionStatusClasses;

  ngOnInit(): void {
    this.reload();
  }

  runBilling(): void {
    this.api.runBilling().subscribe(() => {
      this.snackBar.open('Billing cycle ran', 'Dismiss', { duration: 3000 });
      this.reload();
    });
  }

  newPlan(): void {
    this.dialog.open(CreatePlanDialog, { width: '460px' }).afterClosed().subscribe(request => {
      if (!request) {
        return;
      }
      this.api.createPlan(request).subscribe({
        next: () => {
          this.snackBar.open('Plan created', 'Dismiss', { duration: 3000 });
          this.reload();
        },
        error: err => this.snackBar.open(err.error?.detail ?? 'Could not create plan', 'Dismiss', { duration: 3000 }),
      });
    });
  }

  toggle(plan: Plan): void {
    this.api.setPlanActive(plan.id, !plan.active).subscribe(() => this.reload());
  }

  private reload(): void {
    this.api.adminPlans().subscribe(plans => this.plans.set(plans));
    this.api.adminSubscriptions(0, 100).subscribe(page => this.subscriptions.set(page.content));
  }
}
