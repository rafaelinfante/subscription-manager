import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { CurrencyPipe, LowerCasePipe } from '@angular/common';
import { Plan } from '../../core/models';

export interface SubscribeResult {
  paymentMethodToken: string;
  trial: boolean;
}

@Component({
  selector: 'app-subscribe-dialog',
  imports: [
    ReactiveFormsModule, CurrencyPipe, LowerCasePipe, MatDialogModule, MatFormFieldModule,
    MatSelectModule, MatCheckboxModule, MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>Subscribe to {{ data.plan.name }}</h2>
    <mat-dialog-content class="flex flex-col gap-2">
      <p class="text-slate-500 text-sm mb-2">
        {{ data.plan.amountCents / 100 | currency: data.plan.currency }} / {{ data.plan.billingInterval | lowercase }}
      </p>
      <form [formGroup]="form" class="flex flex-col gap-2">
        <mat-form-field appearance="outline">
          <mat-label>Payment method</mat-label>
          <mat-select formControlName="paymentMethodToken">
            <mat-option value="tok_visa">Visa ending 4242 (approves)</mat-option>
            <mat-option value="tok_declined">Test card (always declines)</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-checkbox formControlName="trial">Start with a 14-day trial</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" (click)="confirm()">Subscribe</button>
    </mat-dialog-actions>
  `,
})
export class SubscribeDialog {
  protected readonly data = inject<{ plan: Plan }>(MAT_DIALOG_DATA);
  private dialogRef = inject(MatDialogRef<SubscribeDialog, SubscribeResult>);
  private fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    paymentMethodToken: 'tok_visa',
    trial: false,
  });

  confirm(): void {
    this.dialogRef.close(this.form.getRawValue());
  }
}
