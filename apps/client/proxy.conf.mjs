// Angular dev-server proxy.
//
// The browser only ever talks to the dev server (localhost:4201), which forwards
// every /api request to the backend. This keeps browser requests same-origin, so
// cookie sessions and CSRF work without CORS and without cross-origin credentials.
//
// The backend target can be overridden without editing this file, which is useful
// when explicitly running the backend on a different local port:
//   API_PROXY_TARGET=http://127.0.0.1:8084 npm start
const target = process.env.API_PROXY_TARGET || 'http://127.0.0.1:8080';

export default {
  '/api/**': {
    target,
    // The local backend runs plain HTTP, so certificate checks do not apply.
    secure: false,
    // Send the target host upstream. Session and CSRF cookies carry no Domain,
    // so the browser still scopes them to the dev-server origin.
    changeOrigin: true,
  },
};
