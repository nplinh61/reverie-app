package com.reverie.account;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reverie.ratelimit.RateLimitGuard;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link RegistrationService} orchestration, focused on the
 * best-effort email behavior: once the account is committed, a send failure must
 * not fail the registration.
 */
class RegistrationServiceTest {

    private final AccountCreator accountCreator = mock(AccountCreator.class);
    private final AccountMailService mailService = mock(AccountMailService.class);
    private final RateLimitGuard rateLimitGuard = mock(RateLimitGuard.class);

    private final RegistrationService service =
            new RegistrationService(accountCreator, mailService, rateLimitGuard);

    private final RegistrationCommand command =
            new RegistrationCommand("alice@example.com", "correct horse battery", "Alice", "alice");

    @Test
    void registrationSucceedsEvenWhenTheVerificationEmailFails() {
        when(accountCreator.createAccount(any(), any(), any(), any()))
                .thenReturn(new IssuedVerification("alice@example.com", "Alice", "raw-token"));
        doThrow(new IllegalStateException("smtp down"))
                .when(mailService).sendVerificationEmail(any(), any(), any());

        // The account is already committed, so this must not propagate.
        assertThatCode(() -> service.register(command, "127.0.0.1")).doesNotThrowAnyException();
        verify(mailService).sendVerificationEmail("alice@example.com", "Alice", "raw-token");
    }

    @Test
    void duplicateEmailStaysNeutralEvenWhenTheNoticeEmailFails() {
        when(accountCreator.createAccount(any(), any(), any(), any()))
                .thenThrow(new EmailAlreadyRegisteredException());
        doThrow(new IllegalStateException("smtp down"))
                .when(mailService).sendAccountExistsNotice(any());

        assertThatCode(() -> service.register(command, "127.0.0.1")).doesNotThrowAnyException();
        verify(mailService).sendAccountExistsNotice("alice@example.com");
        verify(mailService, never()).sendVerificationEmail(any(), any(), any());
    }

    @Test
    void takenUsernamePropagatesAsAConflict() {
        when(accountCreator.createAccount(any(), any(), any(), any()))
                .thenThrow(new UsernameAlreadyTakenException());

        assertThatThrownBy(() -> service.register(command, "127.0.0.1"))
                .isInstanceOf(UsernameAlreadyTakenException.class);
        verify(mailService, never()).sendVerificationEmail(any(), any(), any());
    }
}
