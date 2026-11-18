// Production environment (default build configuration).
//
// apiBaseUrl is intentionally empty, which means the frontend expects the API
// to be reachable at the SAME ORIGIN, served behind a reverse proxy that routes
// "/api/**" to the backend. A real deployment must either set apiBaseUrl to the
// backend's absolute URL or provide an equivalent reverse-proxy or ingress
// setup. The empty value is a placeholder only, not a working production value.
export const environment = {
  apiBaseUrl: '',
};
