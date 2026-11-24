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
    // XSRF: read the REVERIE-XSRF-TOKEN cookie and echo it as X-REVERIE-XSRF-TOKEN
    // on mutating, same-origin requests. The names are project-specific (not
    // Angular's XSRF-TOKEN default) so the cookie never collides with another app
    // sharing the localhost dev host, and they must match Spring Security's
    // configured cookie and header names (ADR-003, SecurityConfig).
    provideHttpClient(
      withXsrfConfiguration({
        cookieName: 'REVERIE-XSRF-TOKEN',
        headerName: 'X-REVERIE-XSRF-TOKEN',
      }),
    ),
    provideIonicAngular({}),
  ],
};
