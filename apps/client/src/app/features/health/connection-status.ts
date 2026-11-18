import { Component, OnInit, inject, signal } from '@angular/core';
import { IonBadge, IonSpinner, IonText } from '@ionic/angular';
import { HealthService } from '../../core/health/health.service';

type ConnectionState = 'loading' | 'connected' | 'error';

/**
 * Shows whether the frontend can reach the backend API. It asks HealthService
 * once when it is created and reflects the outcome in three states: loading
 * while the request is in flight, connected only when the backend answers with
 * status "ok", and error for any other response or failure.
 */
@Component({
  selector: 'app-connection-status',
  imports: [IonSpinner, IonText, IonBadge],
  templateUrl: './connection-status.html',
  styleUrl: './connection-status.scss',
})
export class ConnectionStatus implements OnInit {
  private readonly health = inject(HealthService);

  protected readonly state = signal<ConnectionState>('loading');

  ngOnInit(): void {
    this.health.ping().subscribe({
      next: (response) => {
        this.state.set(response.status === 'ok' ? 'connected' : 'error');
      },
      error: () => {
        this.state.set('error');
      },
    });
  }
}
