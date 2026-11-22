import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { VerifyEmail } from './verify-email';
import { AuthService } from '../../../core/auth/auth.service';

describe('VerifyEmail', () => {
  function setup(token: string | null, authMock: Partial<AuthService>) {
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
});
