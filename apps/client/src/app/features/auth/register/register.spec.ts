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

  it('returns from the check-email view to the form via Back to Reverie', () => {
    const register = vi.fn().mockReturnValue(of({ message: 'ok' }));
    const { fixture, component } = setup({ register } as unknown as AuthService);

    component.form.setValue(validValue);
    component.submit();
    fixture.detectChanges();
    expect(component.state()).toBe('success');

    const el = fixture.nativeElement as HTMLElement;
    const back = el.querySelector('.auth-back') as HTMLButtonElement | null;
    expect(back?.textContent).toContain('Back to Reverie');
    // The check-email view is rendered by the /register route itself, so this is a
    // button that resets the view rather than a link. It must never be a login link.
    expect(el.textContent?.toLowerCase()).not.toContain('log in');

    back?.click();
    fixture.detectChanges();

    // Back on the registration form, not the success view.
    expect(component.state()).toBe('idle');
    expect(el.querySelector('#email')).not.toBeNull();
    expect(el.querySelector('.auth-back')).toBeNull();
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

  it('renders the three photo polaroids with lazy loading', () => {
    const { fixture } = setup({ register: vi.fn() } as unknown as AuthService);
    const el = fixture.nativeElement as HTMLElement;
    const imgs = Array.from(el.querySelectorAll('img.polaroid__img'));
    expect(imgs).toHaveLength(3);

    const srcs = imgs.map((img) => img.getAttribute('src'));
    expect(srcs).toEqual([
      'auth/polaroids/friends-mirror.webp',
      'auth/polaroids/cherry-blossoms.webp',
      'auth/polaroids/ferris-wheel.webp',
    ]);
    // Reserved dimensions and lazy loading avoid layout shift and needless fetches.
    for (const img of imgs) {
      expect(img.getAttribute('loading')).toBe('lazy');
      expect(img.getAttribute('width')).toBeTruthy();
      expect(img.getAttribute('height')).toBeTruthy();
    }
  });

  it('keeps all four registration fields', () => {
    const { fixture } = setup({ register: vi.fn() } as unknown as AuthService);
    const el = fixture.nativeElement as HTMLElement;
    for (const id of ['email', 'displayName', 'username', 'password']) {
      expect(el.querySelector(`#${id}`)).not.toBeNull();
    }
  });

  it('renders no login link (login is out of scope)', () => {
    const { fixture } = setup({ register: vi.fn() } as unknown as AuthService);
    const el = fixture.nativeElement as HTMLElement;
    const anchors = Array.from(el.querySelectorAll('a'));
    expect(anchors).toHaveLength(0);
    expect(el.textContent?.toLowerCase()).not.toContain('log in');
  });

  it('toggles password visibility without losing the value', () => {
    const { fixture, component } = setup({
      register: vi.fn(),
    } as unknown as AuthService);
    const el = fixture.nativeElement as HTMLElement;
    const input = el.querySelector('#password') as HTMLInputElement;
    const toggle = el.querySelector('.auth-affix-btn') as HTMLButtonElement;

    expect(input.getAttribute('type')).toBe('password');

    toggle.click();
    fixture.detectChanges();
    expect(input.getAttribute('type')).toBe('text');
    expect(component.passwordVisible()).toBe(true);

    toggle.click();
    fixture.detectChanges();
    expect(input.getAttribute('type')).toBe('password');
  });
});
