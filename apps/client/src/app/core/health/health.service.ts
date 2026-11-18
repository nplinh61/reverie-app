import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PingResponse } from '../../shared/models/ping-response';

/**
 * Talks to the backend health endpoint. This service is the only place that
 * knows the API URL, so presentation code never builds URLs or calls HttpClient
 * directly. The base URL comes from the active environment configuration.
 */
@Injectable({ providedIn: 'root' })
export class HealthService {
  private readonly http = inject(HttpClient);

  /**
   * Calls GET {apiBaseUrl}/api/v1/ping. Emits the parsed response or errors
   * through the Observable so the caller can handle failures.
   */
  ping(): Observable<PingResponse> {
    return this.http.get<PingResponse>(`${environment.apiBaseUrl}/api/v1/ping`);
  }
}
