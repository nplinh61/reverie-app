import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { NEVER, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { VerifyEmail } from './verify-email';
import { AuthService } from '../../../core/auth/auth.service';

describe('VerifyEmail', () => {
  function setup(token: string | null, authMock: Partial<AuthService>) {
    // Reset first so setup can be called more than once within a single test.
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [VerifyEmail],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authMock },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => token } } },
        },
      ],
    });
    const fixture = TestBed.createComponent(VerifyEmail);
    fixture.detectChanges(); // runs ngOnInit
    return { fixture, component: fixture.componentInstance as any };
  }

  it('reports a missing token without calling the service', () => {
    const verifyEmail = vi.fn();
    const { component } = setup(null, { verifyEmail } as unknown as AuthService);

    expect(verifyEmail).not.toHaveBeenCalled();
    expect(component.state()).toBe('missing');
  });

  it('shows the loading state while verification is in flight', () => {
    const verifyEmail = vi.fn().mockReturnValue(NEVER);
    const { fixture, component } = setup('raw-token', {
      verifyEmail,
    } as unknown as AuthService);

    expect(component.state()).toBe('verifying');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.verify-spinner')).not.toBeNull();
  });

  it('shows success when the token is accepted', () => {
    const verifyEmail = vi.fn().mockReturnValue(of({ message: 'Your email address has been verified.' }));
    const { component } = setup('raw-token', { verifyEmail } as unknown as AuthService);

    expect(verifyEmail).toHaveBeenCalledWith('raw-token');
    expect(component.state()).toBe('success');
    expect(component.message()).toContain('verified');
  });

  it('shows the backend message on an invalid token', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        status: 400,
        code: 'INVALID_VERIFICATION_TOKEN',
        message: 'This verification link is invalid or has expired. Please request a new one.',
        fieldErrors: [],
      },
    });
    const verifyEmail = vi.fn().mockReturnValue(throwError(() => error));
    const { component } = setup('bad-token', { verifyEmail } as unknown as AuthService);

    expect(component.state()).toBe('error');
    expect(component.message()).toContain('invalid or has expired');
  });

  it('offers Back to Reverie navigation to /register on resolved states', () => {
    const cases: Array<[string | null, Partial<AuthService>]> = [
      ['raw-token', { verifyEmail: vi.fn().mockReturnValue(of({ message: 'Verified.' })) }], // success
      [null, { verifyEmail: vi.fn() }], // missing token
      [
        'bad-token',
        {
          verifyEmail: vi
            .fn()
            .mockReturnValue(throwError(() => new HttpErrorResponse({ status: 400, error: null }))),
        },
      ], // error
    ];

    for (const [token, authMock] of cases) {
      const { fixture } = setup(token, authMock as AuthService);
      const el = fixture.nativeElement as HTMLElement;
      const back = el.querySelector('.auth-back') as HTMLAnchorElement | null;
      expect(back?.textContent).toContain('Back to Reverie');
      expect(back?.getAttribute('href')).toBe('/register');
    }
  });

  it('shows no navigation link while verification is in flight', () => {
    const { fixture } = setup('raw-token', {
      verifyEmail: vi.fn().mockReturnValue(NEVER),
    } as unknown as AuthService);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.auth-back')).toBeNull();
  });
});
