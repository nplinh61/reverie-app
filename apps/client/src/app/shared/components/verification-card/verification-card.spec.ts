import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { VerificationCard } from './verification-card';

@Component({
  imports: [VerificationCard],
  template: `<app-verification-card><p class="probe">Hello</p></app-verification-card>`,
})
class Host {}

describe('VerificationCard', () => {
  it('projects its content inside the card and shows the wordmark', () => {
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.auth-card .probe')?.textContent).toBe('Hello');
    expect(el.querySelector('app-reverie-wordmark')).not.toBeNull();
  });

  it('exposes the card as a polite live region', () => {
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.auth-card')?.getAttribute('aria-live')).toBe(
      'polite',
    );
  });
});
