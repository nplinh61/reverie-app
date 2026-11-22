import { TestBed } from '@angular/core/testing';
import {
  provideHttpClient,
  withXsrfConfiguration,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { RegisterRequest } from '../../shared/models/auth';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(
          withXsrfConfiguration({
            cookieName: 'XSRF-TOKEN',
            headerName: 'X-XSRF-TOKEN',
          }),
        ),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('initializes CSRF before posting a registration', () => {
    const request: RegisterRequest = {
      email: 'alice@example.com',
      password: 'correct horse',
      displayName: 'Alice',
      username: 'alice',
    };
    let result: { message: string } | undefined;

    service.register(request).subscribe((response) => {
      result = response;
    });

    // The CSRF bootstrap happens first.
    const csrf = httpTesting.expectOne(`${environment.apiBaseUrl}/api/v1/auth/csrf`);
    expect(csrf.request.method).toBe('GET');
    csrf.flush(null);

    // Only then does the registration POST go out, carrying the body.
    const post = httpTesting.expectOne(
      `${environment.apiBaseUrl}/api/v1/auth/register`,
    );
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(request);
    post.flush({ message: 'ok' });

    expect(result).toEqual({ message: 'ok' });
  });

  it('posts the token to the verify endpoint after CSRF init', () => {
    service.verifyEmail('raw-token').subscribe();

    httpTesting.expectOne(`${environment.apiBaseUrl}/api/v1/auth/csrf`).flush(null);

    const post = httpTesting.expectOne(
      `${environment.apiBaseUrl}/api/v1/auth/verify-email`,
    );
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ token: 'raw-token' });
    post.flush({ message: 'verified' });
  });

  it('posts the email to the resend endpoint after CSRF init', () => {
    service.resendVerification('alice@example.com').subscribe();

    httpTesting.expectOne(`${environment.apiBaseUrl}/api/v1/auth/csrf`).flush(null);

    const post = httpTesting.expectOne(
      `${environment.apiBaseUrl}/api/v1/auth/resend-verification`,
    );
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ email: 'alice@example.com' });
    post.flush({ message: 'ok' });
  });
});
