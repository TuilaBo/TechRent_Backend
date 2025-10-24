package com.rentaltech.techrental.authentication.service;

import com.rentaltech.techrental.authentication.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountService {
    List<Account> getAllAccounts();
    Optional<Account> getAccountById(Long id);
    Account addAccount(Account account);

    void setVerificationCodeAndSendEmail(Account account);
    boolean verifyEmail(String email, String code);

    Optional<Account> getByUsername(String username);

    Account getByEmail(String email);

    Account updateAccount(Account account);

    void deleteAccount(Long accountId);
}
