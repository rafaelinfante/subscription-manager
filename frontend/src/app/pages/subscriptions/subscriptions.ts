import { Component, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Api } from '../../core/api';
import { Plan, Subscription } from '../../core/models';
import { subscriptionStatusClasses } from '../../core/format';

@Component({
  selector: 'app-subscriptions',
  imports: [CurrencyPipe, DatePipe, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatMenuModule],
  template: `
    <h1 class="text-2xl font-semibold mb-6">Your subscriptions</h1>

    @if (subscriptions().length) {
      <div class="grid gap-4 lg:grid-cols-2">
        @for (sub of subscriptions(); track sub.id) {
          <mat-card class="p-5">
            <div class="flex items-start justify-between">
              <div>
                <h2 class="text-lg font-semibold">{{ sub.plan.name }}</h2>
                <p class="text-slate-500 text-sm">
                  {{ sub.plan.amountCents / 100 | currency: sub.plan.currency }} / {{ sub.plan.billingInterval.toLowerCase() }}
                </p>
              </div>
              <span class="px-2 py-0.5 rounded-full text-xs font-medium" [class]="statusClasses(sub.status)">
                {{ sub.status }}
              </span>
            </div>

            <dl class="grid grid-cols-2 gap-y-1 text-sm mt-4 text-slate-600">
              <dt class="text-slate-400">Current period</dt>
              <dd>{{ sub.currentPeriodStart | date: 'mediumDate' }} – {{ sub.currentPeriodEnd | date: 'mediumDate' }}</dd>
              <dt class="text-slate-400">Next billing</dt>
              <dd>{{ sub.nextBillingDate | date: 'mediumDate' }}</dd>
              <dt class="text-slate-400">Payment method</dt>
              <dd class="font-mono text-xs">{{ sub.paymentMethodToken }}</dd>
            </dl>

            @if (sub.cancelAtPeriodEnd) {
              <p class="text-xs text-amber-700 bg-amber-50 rounded p-2 mt-3">Ends on {{ sub.currentPeriodEnd | date: 'mediumDate' }}</p>
            }

            @if (canManage(sub)) {
              <div class="flex gap-2 mt-4">
                <button mat-stroked-button [matMenuTriggerFor]="planMenu">
                  <mat-icon>swap_horiz</mat-icon> Change plan
                </button>
                <mat-menu #planMenu>
                  @for (plan of otherPlans(sub); track plan.id) {
                    <button mat-menu-item (click)="changePlan(sub, plan)">{{ plan.name }}</button>
                  }
                </mat-menu>

                <button mat-stroked-button [matMenuTriggerFor]="cancelMenu">
                  <mat-icon>cancel</mat-icon> Cancel
                </button>
                <mat-menu #cancelMenu>
                  <button mat-menu-item (click)="cancel(sub, true)">At period end</button>
                  <button mat-menu-item (click)="cancel(sub, false)">Immediately</button>
                </mat-menu>
              </div>
            }
          </mat-card>
        }
      </div>
    } @else {
      <mat-card class="p-8 text-center text-slate-500">
        <p class="mb-3">You have no subscriptions yet.</p>
        <a mat-flat-button color="primary" routerLink="/plans">Browse plans</a>
      </mat-card>
    }
  `,
})
export class Subscriptions implements OnInit {
  private api = inject(Api);
  private snackBar = inject(MatSnackBar);

  protected readonly subscriptions = signal<Subscription[]>([]);
  protected readonly plans = signal<Plan[]>([]);
  protected readonly statusClasses = subscriptionStatusClasses;

  ngOnInit(): void {
    this.reload();
    this.api.plans().subscribe(plans => this.plans.set(plans));
  }

  canManage(sub: Subscription): boolean {
    return sub.status === 'ACTIVE' || sub.status === 'TRIALING' || sub.status === 'PAST_DUE';
  }

  otherPlans(sub: Subscription): Plan[] {
    return this.plans().filter(plan => plan.id !== sub.plan.id);
  }

  changePlan(sub: Subscription, plan: Plan): void {
    this.api.changePlan(sub.id, plan.id).subscribe({
      next: () => this.afterChange(`Moved to ${plan.name}`),
      error: err => this.error(err),
    });
  }

  cancel(sub: Subscription, atPeriodEnd: boolean): void {
    this.api.cancel(sub.id, atPeriodEnd).subscribe({
      next: () => this.afterChange(atPeriodEnd ? 'Will cancel at period end' : 'Subscription canceled'),
      error: err => this.error(err),
    });
  }

  private afterChange(message: string): void {
    this.snackBar.open(message, 'Dismiss', { duration: 3000 });
    this.reload();
  }

  private error(err: { error?: { detail?: string } }): void {
    this.snackBar.open(err.error?.detail ?? 'Something went wrong', 'Dismiss', { duration: 3000 });
  }

  private reload(): void {
    this.api.subscriptions(0, 100).subscribe(page => this.subscriptions.set(page.content));
  }
}
