// Development environment.
//
// The backend runs natively on the Windows host on its default port 8080, while
// the Angular dev server runs on http://localhost:4200. Calls therefore cross
// origins and exercise the backend CORS allowlist on purpose.
export const environment = {
  apiBaseUrl: 'http://localhost:8080',
};
