import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Register } from './register';
import { AuthService } from '../../../core/auth/auth.service';

describe('Register', () => {
  function setup(authMock: Partial<AuthService>) {
    TestBed.configureTestingModule({
      imports: [Register],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authMock },
      ],
    });
    const fixture = TestBed.createComponent(Register);
    fixture.detectChanges();
    // Protected members are reached through a cast for testing only.
    return { fixture, component: fixture.componentInstance as any };
  }

  const validValue = {
    email: 'alice@example.com',
    displayName: 'Alice',
    username: 'alice',
    password: 'correct horse',
  };

  it('does not call the service for an invalid form', () => {
    const register = vi.fn();
    const { component } = setup({ register } as unknown as AuthService);

    component.submit();

    expect(register).not.toHaveBeenCalled();
    expect(component.state()).toBe('idle');
  });

  it('shows the check-email view on success', () => {
    const register = vi.fn().mockReturnValue(of({ message: 'ok' }));
    const { component } = setup({ register } as unknown as AuthService);

    component.form.setValue(validValue);
    component.submit();

    expect(register).toHaveBeenCalledOnce();
    expect(component.state()).toBe('success');
    expect(component.submittedEmail()).toBe('alice@example.com');
  });

  it('maps a backend username field error onto the field', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        status: 400,
        code: 'VALIDATION_FAILED',
        message: 'Request validation failed.',
        fieldErrors: [
          { field: 'username', message: 'This username is already taken.' },
        ],
      },
    });
    const register = vi.fn().mockReturnValue(throwError(() => error));
    const { component } = setup({ register } as unknown as AuthService);

    component.form.setValue(validValue);
    component.submit();

    expect(component.state()).toBe('error');
    expect(component.fieldError('username')).toBe('This username is already taken.');
  });

  it('shows a generic message when rate limited', () => {
    const error = new HttpErrorResponse({ status: 429, error: null });
    const register = vi.fn().mockReturnValue(throwError(() => error));
    const { component } = setup({ register } as unknown as AuthService);

    component.form.setValue(validValue);
    component.submit();

    expect(component.state()).toBe('error');
    expect(component.generalError()).toContain('Too many attempts');
  });
});
