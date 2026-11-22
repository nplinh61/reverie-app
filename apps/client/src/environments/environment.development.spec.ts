import { environment } from './environment.development';

describe('development environment', () => {
  it('uses a relative API base URL so dev requests are same-origin', () => {
    // An empty base URL makes HealthService issue relative "/api/**" requests,
    // which the dev-server proxy forwards to the backend. This is what keeps
    // cookie sessions and CSRF working without CORS in later milestones.
    expect(environment.apiBaseUrl).toBe('');
  });
});
