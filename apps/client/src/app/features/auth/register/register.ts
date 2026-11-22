import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { ApiError, RegisterRequest } from '../../../shared/models/auth';

type RegisterState = 'idle' | 'submitting' | 'success' | 'error';
type ResendState = 'idle' | 'sending' | 'sent' | 'error';

/**
 * Registration screen. It mirrors the backend validation rules for quick
 * feedback, but the backend stays authoritative: field-level errors it returns
 * (for example a taken username) are shown against the matching input. On
 * success it switches to a neutral "check your email" view with a resend
 * action, matching the backend's neutral responses.
 */
@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  protected readonly state = signal<RegisterState>('idle');
  protected readonly resendState = signal<ResendState>('idle');
  protected readonly generalError = signal<string | null>(null);
  protected readonly fieldErrors = signal<Record<string, string>>({});
  protected readonly submittedEmail = signal<string>('');

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    displayName: ['', [Validators.required, Validators.maxLength(100)]],
    username: [
      '',
      [Validators.required, Validators.pattern(/^[a-z0-9_]{3,30}$/)],
    ],
    password: ['', [Validators.required, Validators.minLength(10)]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.state.set('submitting');
    this.generalError.set(null);
    this.fieldErrors.set({});

    const request = this.form.getRawValue() as RegisterRequest;
    this.auth.register(request).subscribe({
      next: () => {
        this.submittedEmail.set(request.email);
        this.state.set('success');
      },
      error: (error: HttpErrorResponse) => {
        this.applyError(error);
        this.state.set('error');
      },
    });
  }

  protected resend(): void {
    const email = this.submittedEmail();
    if (!email) {
      return;
    }
    this.resendState.set('sending');
    this.auth.resendVerification(email).subscribe({
      next: () => this.resendState.set('sent'),
      error: () => this.resendState.set('error'),
    });
  }

  protected fieldError(field: string): string | undefined {
    return this.fieldErrors()[field];
  }

  private applyError(error: HttpErrorResponse): void {
    if (error.status === 429) {
      this.generalError.set('Too many attempts. Please wait a moment and try again.');
      return;
    }

    const body = error.error as ApiError | null;
    if (body && Array.isArray(body.fieldErrors) && body.fieldErrors.length > 0) {
      const mapped: Record<string, string> = {};
      for (const fieldError of body.fieldErrors) {
        mapped[fieldError.field] = fieldError.message;
      }
      this.fieldErrors.set(mapped);
      this.generalError.set('Please correct the highlighted fields.');
      return;
    }

    this.generalError.set(
      'Something went wrong while creating your account. Please try again.',
    );
  }
}
