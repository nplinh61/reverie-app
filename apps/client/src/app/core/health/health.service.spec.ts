import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { HealthService } from './health.service';

describe('HealthService', () => {
  let service: HealthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(HealthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should call GET {apiBaseUrl}/api/v1/ping and return the response', () => {
    let result: { status: string } | undefined;

    service.ping().subscribe((response) => {
      result = response;
    });

    const req = httpTesting.expectOne(
      `${environment.apiBaseUrl}/api/v1/ping`,
    );
    expect(req.request.method).toBe('GET');

    req.flush({ status: 'ok' });

    expect(result).toEqual({ status: 'ok' });
  });
});
