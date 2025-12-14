package com.rentaltech.techrental.authentication.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.repository.AccountRepository;
import com.rentaltech.techrental.security.JwtTokenProvider;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.CustomerStatus;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private VerificationEmailService verificationEmailService;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    void addAccountRejectsDuplicateUsername() {
        Account existing = Account.builder().accountId(1L).build();
        when(accountRepository.findByUsername("dup")).thenReturn(existing);

        Account account = baseAccount();
        account.setUsername("dup");

        assertThatThrownBy(() -> accountService.addAccount(account))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tên đăng nhập đã tồn tại");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void addAccountDeletesInactiveEmailBeforeSaving() {
        Account inactive = Account.builder()
                .accountId(99L)
                .email("inactive@example.com")
                .isActive(false)
                .build();

        when(accountRepository.findByUsername("user")).thenReturn(null);
        when(accountRepository.findByEmail("inactive@example.com")).thenReturn(inactive);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(any())).thenAnswer(invocation -> "ENC-" + invocation.getArgument(0));

        Account account = baseAccount();
        account.setEmail("inactive@example.com");
        account.setRole(Role.CUSTOMER);
        account.setIsActive(true);

        accountService.addAccount(account);

        verify(accountRepository).delete(inactive);
        ArgumentCaptor<Account> savedCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getPassword()).isEqualTo("ENC-password");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void verifyEmailActivatesAccountAndCustomer() {
        Account account = Account.builder()
                .accountId(10L)
                .email("user@example.com")
                .role(Role.CUSTOMER)
                .verificationExpiry(LocalDateTime.now().plusMinutes(5))
                .build();

        when(accountRepository.findByEmailAndVerificationCode("user@example.com", "123456"))
                .thenReturn(account);
        when(customerRepository.findByAccount_AccountId(10L))
                .thenReturn(Optional.of(Customer.builder().customerId(20L).status(CustomerStatus.INACTIVE).build()));

        accountService.verifyEmail("user@example.com", "123456");

        assertThat(account.getIsActive()).isTrue();
        assertThat(account.getVerificationCode()).isNull();
        verify(accountRepository).save(account);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void resendVerificationCodeRejectsActiveAccount() {
        Account account = Account.builder()
                .accountId(5L)
                .email("active@example.com")
                .isActive(true)
                .build();
        when(accountRepository.findByEmail("active@example.com")).thenReturn(account);

        assertThatThrownBy(() -> accountService.resendVerificationCode("active@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("được xác thực");
        verify(verificationEmailService, never()).sendVerificationEmail(any(), any());
    }

    @Test
    void forgotPasswordThrowsWhenAccountInactive() {
        Account account = Account.builder()
                .accountId(7L)
                .email("inactive@example.com")
                .isActive(false)
                .build();
        when(accountRepository.findByEmail("inactive@example.com")).thenReturn(account);

        assertThatThrownBy(() -> accountService.forgotPassword("inactive@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chưa được kích hoạt");
    }

    @Test
    void forgotPasswordSendsEmailWhenAccountActive() {
        // given
        Account account = Account.builder()
                .accountId(11L)
                .email("active@example.com")
                .isActive(true)
                .build();
        when(accountRepository.findByEmail("active@example.com")).thenReturn(account);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        accountService.forgotPassword("active@example.com");

        // then
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();
        assertThat(saved.getResetPasswordCode()).isNotNull();
        assertThat(saved.getResetPasswordExpiry()).isNotNull();
        verify(verificationEmailService).sendResetPasswordEmail("active@example.com", any());
    }

    @Test
    void forgotPasswordNoOpWhenEmailUnknown() {
        // given
        when(accountRepository.findByEmail("unknown@example.com")).thenReturn(null);

        // when
        accountService.forgotPassword("unknown@example.com");

        // then
        verify(accountRepository, never()).save(any());
        verify(verificationEmailService, never()).sendResetPasswordEmail(any(), any());
    }

    @Test
    void resetPasswordRejectsExpiredCode() {
        Account account = Account.builder()
                .accountId(8L)
                .email("user@example.com")
                .resetPasswordExpiry(LocalDateTime.now().minusMinutes(1))
                .build();

        when(accountRepository.findByEmailAndResetPasswordCode("user@example.com", "000000"))
                .thenReturn(account);

        assertThatThrownBy(() -> accountService.resetPassword("user@example.com", "000000", "newpass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã hết hạn");
        verify(passwordEncoder, never()).encode(any());
    }

    private Account baseAccount() {
        return Account.builder()
                .username("user")
                .password("password")
                .email("user@example.com")
                .phoneNumber("0900000000")
                .role(Role.ADMIN)
                .isActive(true)
                .build();
    }
}
