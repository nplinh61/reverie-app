// Development environment.
//
// apiBaseUrl is empty so the frontend issues relative "/api/**" requests. The
// Angular dev server proxies those to the backend (see proxy.conf.mjs), which
// makes all browser traffic same-origin on http://localhost:4200. This is what
// lets cookie sessions and CSRF work in later milestones without CORS or
// cross-origin credentials.
export const environment = {
  apiBaseUrl: '',
};
