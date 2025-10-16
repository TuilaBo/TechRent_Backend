package com.rentaltech.techrental.authentication.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountServiceImpl implements AccountService {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private JavaMailSender mailSender;

    @Override
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Override
    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }

    @Override
    public Account addAccount(Account account) {
        if (accountRepository.findByUsername(account.getUsername()) != null) {
            throw new RuntimeException("Username already exists!");
        }
        if (account.getEmail() != null && accountRepository.findByEmail(account.getEmail()) != null) {
            throw new RuntimeException("Email already exists!");
        }
        if (account.getPhoneNumber() != null && accountRepository.findByPhoneNumber(account.getPhoneNumber()) != null) {
            throw new RuntimeException("Phone number already exists!");
        }
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        return accountRepository.save(account);
    }

    @Override
    public void setVerificationCodeAndSendEmail(Account account) {
        String code = String.format("%06d", new java.util.Random().nextInt(1_000_000));
        account.setVerificationCode(code);
        account.setVerificationExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        accountRepository.save(account);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(account.getEmail());
        message.setSubject("Verify your email");
        message.setText("Your verification code is: " + code + "\nThis code expires in 10 minutes.");
        mailSender.send(message);
    }

    @Override
    public boolean verifyEmail(String email, String code) {
        Account account = accountRepository.findByEmailAndVerificationCode(email, code);
        if (account == null) {
            return false;
        }
        if (account.getVerificationExpiry() == null || account.getVerificationExpiry().isBefore(java.time.LocalDateTime.now())) {
            return false;
        }
        account.setIsActive(true);
        account.setVerificationCode(null);
        account.setVerificationExpiry(null);
        accountRepository.save(account);
        return true;
    }

    @Override
    public Optional<Account> getByUsername(String username) {
        return Optional.ofNullable(accountRepository.findByUsername(username));
    }


}
