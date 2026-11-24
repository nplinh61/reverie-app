import { TestBed } from '@angular/core/testing';
import { ReverieWordmark } from './reverie-wordmark';

describe('ReverieWordmark', () => {
  it('renders the Reverie wordmark text', () => {
    const fixture = TestBed.createComponent(ReverieWordmark);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.wordmark__text')?.textContent).toBe('Reverie');
  });

  it('marks the decorative mark as hidden from assistive tech', () => {
    const fixture = TestBed.createComponent(ReverieWordmark);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
  });
});
