import { Component } from '@angular/core';
import { ReverieWordmark } from '../reverie-wordmark/reverie-wordmark';

/**
 * A centered confirmation card on the soft lavender-pink background. It carries
 * the Reverie wordmark and projects its content, so every email-verification
 * state (check email, resend, success, expired or invalid, loading, error) and
 * the registration "check your email" view share one gentle, consistent layout
 * instead of repeating the surface and card markup.
 *
 * The card is an aria-live polite region so state changes are announced to
 * assistive technology without stealing focus.
 */
@Component({
  selector: 'app-verification-card',
  imports: [ReverieWordmark],
  templateUrl: './verification-card.html',
  styleUrl: './verification-card.scss',
})
export class VerificationCard {}
