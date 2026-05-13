import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { CreatePlanRequest } from '../../core/api';

@Component({
  selector: 'app-create-plan-dialog',
  imports: [
    ReactiveFormsModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>New plan</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="flex flex-col gap-2 pt-2">
        <mat-form-field appearance="outline">
          <mat-label>Code</mat-label>
          <input matInput formControlName="code" placeholder="team_monthly" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Name</mat-label>
          <input matInput formControlName="name" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <input matInput formControlName="description" />
        </mat-form-field>
        <div class="flex gap-2">
          <mat-form-field appearance="outline" class="flex-1">
            <mat-label>Price (USD)</mat-label>
            <input matInput type="number" min="1" formControlName="amount" />
          </mat-form-field>
          <mat-form-field appearance="outline" class="flex-1">
            <mat-label>Interval</mat-label>
            <mat-select formControlName="billingInterval">
              <mat-option value="MONTH">Monthly</mat-option>
              <mat-option value="YEAR">Yearly</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="confirm()">Create</button>
    </mat-dialog-actions>
  `,
})
export class CreatePlanDialog {
  private dialogRef = inject(MatDialogRef<CreatePlanDialog, CreatePlanRequest>);
  private fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
    description: [''],
    amount: [10, [Validators.required, Validators.min(1)]],
    billingInterval: ['MONTH' as 'MONTH' | 'YEAR', Validators.required],
  });

  confirm(): void {
    const value = this.form.getRawValue();
    this.dialogRef.close({
      code: value.code,
      name: value.name,
      description: value.description,
      amountCents: Math.round(value.amount * 100),
      currency: 'USD',
      billingInterval: value.billingInterval,
    });
  }
}
