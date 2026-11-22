import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { ApiError } from '../../../shared/models/auth';

type VerifyState = 'verifying' | 'success' | 'error' | 'missing';

/**
 * Landing screen for the verification link in the email. It reads the token from
 * the query string, calls the backend verify endpoint once, and reflects the
 * outcome. The backend decides validity; this screen only presents it.
 */
@Component({
  selector: 'app-verify-email',
  imports: [RouterLink],
  templateUrl: './verify-email.html',
  styleUrl: './verify-email.scss',
})
export class VerifyEmail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);

  protected readonly state = signal<VerifyState>('verifying');
  protected readonly message = signal<string>('');

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state.set('missing');
      return;
    }

    this.auth.verifyEmail(token).subscribe({
      next: (response) => {
        this.message.set(response.message);
        this.state.set('success');
      },
      error: (error: HttpErrorResponse) => {
        const body = error.error as ApiError | null;
        this.message.set(
          body?.message ??
            'This verification link is invalid or has expired. Please request a new one.',
        );
        this.state.set('error');
      },
    });
  }
}
