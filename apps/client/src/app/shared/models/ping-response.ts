/**
 * Shape of the backend liveness response returned by GET /api/v1/ping.
 * The backend answers with a single "status" field whose value is "ok" when
 * the API is reachable.
 */
export interface PingResponse {
  status: string;
}
