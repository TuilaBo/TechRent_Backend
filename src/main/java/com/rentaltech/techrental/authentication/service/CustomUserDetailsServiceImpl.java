package com.rentaltech.techrental.authentication.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.repository.AccountRepository;
import com.rentaltech.techrental.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Transactional
public class CustomUserDetailsServiceImpl implements UserDetailsService {
    private final AccountRepository accountRepository;
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) {
        Account account = accountRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        if (account == null) {
            throw new UsernameNotFoundException("Không tìm thấy người dùng: " + usernameOrEmail);
        }
        return new CustomUserDetails(account);
    }
}
