import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { MessageResponse, RegisterRequest } from '../../shared/models/auth';

/**
 * Talks to the backend account endpoints. This service is the only place that
 * knows those URLs, so components never build URLs or call HttpClient directly.
 *
 * <p>Every mutating call first fetches GET /auth/csrf. That response sets the
 * XSRF-TOKEN cookie, which Angular's built-in XSRF support then echoes as the
 * X-XSRF-TOKEN header on the POST. Because apiBaseUrl is empty the requests are
 * relative and same-origin (through the dev proxy), which is what lets the
 * cookie and header line up without CORS.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  /** Initializes the CSRF cookie before a mutating request. */
  private initCsrf(): Observable<void> {
    return this.http.get<void>(`${this.base}/api/v1/auth/csrf`);
  }

  register(request: RegisterRequest): Observable<MessageResponse> {
    return this.initCsrf().pipe(
      switchMap(() =>
        this.http.post<MessageResponse>(`${this.base}/api/v1/auth/register`, request),
      ),
    );
  }

  verifyEmail(token: string): Observable<MessageResponse> {
    return this.initCsrf().pipe(
      switchMap(() =>
        this.http.post<MessageResponse>(`${this.base}/api/v1/auth/verify-email`, {
          token,
        }),
      ),
    );
  }

  resendVerification(email: string): Observable<MessageResponse> {
    return this.initCsrf().pipe(
      switchMap(() =>
        this.http.post<MessageResponse>(
          `${this.base}/api/v1/auth/resend-verification`,
          { email },
        ),
      ),
    );
  }
}
