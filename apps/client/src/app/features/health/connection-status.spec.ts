import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { ConnectionStatus } from './connection-status';

describe('ConnectionStatus', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ConnectionStatus],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should show the loading state while the request is in flight', () => {
    const fixture = TestBed.createComponent(ConnectionStatus);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Checking backend connection');

    // The request is still open at this point. Flush it so the afterEach
    // verify() does not report an outstanding request.
    httpTesting
      .expectOne(`${environment.apiBaseUrl}/api/v1/ping`)
      .flush({ status: 'ok' });
  });

  it('should show the connected state when the backend returns status "ok"', () => {
    const fixture = TestBed.createComponent(ConnectionStatus);
    fixture.detectChanges();

    httpTesting
      .expectOne(`${environment.apiBaseUrl}/api/v1/ping`)
      .flush({ status: 'ok' });

    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Connected');
  });

  it('should show the error state when the request fails', () => {
    const fixture = TestBed.createComponent(ConnectionStatus);
    fixture.detectChanges();

    httpTesting
      .expectOne(`${environment.apiBaseUrl}/api/v1/ping`)
      .error(new ProgressEvent('network error'));

    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Not connected');
  });
});
