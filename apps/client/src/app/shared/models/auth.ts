// Shared request and response shapes for the account endpoints.
//
// These mirror the backend DTOs (com.reverie.web.auth) and the shared error
// envelope (com.reverie.web.error.ApiError). The error shape has exactly four
// fields; fieldErrors is empty except on validation failures.

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  username: string;
}

export interface MessageResponse {
  message: string;
}

export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiError {
  status: number;
  code: string;
  message: string;
  fieldErrors: ApiFieldError[];
}
