import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ReverieWordmark } from '../../../shared/components/reverie-wordmark/reverie-wordmark';
import { VerificationCard } from '../../../shared/components/verification-card/verification-card';
import { AuthService } from '../../../core/auth/auth.service';
import { ApiError, RegisterRequest } from '../../../shared/models/auth';

type RegisterState = 'idle' | 'submitting' | 'success' | 'error';
type ResendState = 'idle' | 'sending' | 'sent' | 'error';

/**
 * A decorative polaroid in the desktop storytelling panel. The image slot is a
 * local file name under public/auth/polaroids; while no file is present the
 * template shows a gradient placeholder and never requests a missing image.
 * See public/auth/polaroids/README.md for how to add licensed photography.
 */
interface Polaroid {
  readonly caption: string;
  readonly tone: 'sunset' | 'bloom' | 'stage';
  readonly rotation: string;
  /**
   * Optional photograph file name under public/auth/polaroids. While it is
   * undefined the template shows the gradient placeholder and requests no image,
   * so there is never a broken-image icon. The frame uses a fixed aspect ratio
   * and object-fit: cover, so adding a photo needs no layout change.
   */
  readonly image?: string;
  /** Intrinsic pixel size of the photo, used to reserve space and avoid shift. */
  readonly width?: number;
  readonly height?: number;
  /**
   * Per-photo object-position for the cover crop, so each subject stays in frame.
   * Defaults to center when omitted.
   */
  readonly objectPosition?: string;
}

/**
 * Registration screen. It mirrors the backend validation rules for quick
 * feedback, but the backend stays authoritative: field-level errors it returns
 * (for example a taken username) are shown against the matching input. On
 * success it switches to a neutral "check your email" view with a resend
 * action, matching the backend's neutral responses.
 */
@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, ReverieWordmark, VerificationCard],
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
  protected readonly passwordVisible = signal<boolean>(false);

  /** Decorative polaroids for the desktop storytelling panel. */
  protected readonly polaroids: readonly Polaroid[] = [
    {
      caption: "the days we'd replay ♡",
      tone: 'sunset',
      rotation: '4deg',
      image: 'friends-mirror.webp',
      width: 1000,
      height: 667,
      objectPosition: 'center',
    },
    {
      caption: 'little things, lovely days ✿',
      tone: 'bloom',
      rotation: '-6deg',
      image: 'cherry-blossoms.webp',
      width: 750,
      height: 1000,
      objectPosition: 'center',
    },
    {
      caption: 'wish you were here ♡',
      tone: 'stage',
      rotation: '3deg',
      image: 'ferris-wheel.webp',
      width: 667,
      height: 1000,
      objectPosition: '50% 40%',
    },
  ];

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

  protected togglePassword(): void {
    this.passwordVisible.update((visible) => !visible);
  }

  /**
   * Returns from the "check your email" view to the registration form. The view
   * is rendered by the /register route itself, so routing to /register would be a
   * no-op; instead we reset the local view state. The typed form values are kept
   * so the person can adjust and resubmit if needed.
   */
  protected backToForm(): void {
    this.state.set('idle');
    this.resendState.set('idle');
    this.generalError.set(null);
    this.fieldErrors.set({});
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
