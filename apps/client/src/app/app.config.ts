import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import {
  provideHttpClient,
  withXsrfConfiguration,
} from '@angular/common/http';
import { routes } from './app.routes';
import { provideIonicAngular } from '@ionic/angular';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    // XSRF: read the XSRF-TOKEN cookie and echo it as X-XSRF-TOKEN on mutating,
    // same-origin requests. These are Angular's defaults, stated explicitly so
    // they stay aligned with Spring Security's cookie and header names (ADR-003).
    provideHttpClient(
      withXsrfConfiguration({
        cookieName: 'XSRF-TOKEN',
        headerName: 'X-XSRF-TOKEN',
      }),
    ),
    provideIonicAngular({}),
  ],
};
