package com.rentaltech.techrental.authentication.repository;

import com.rentaltech.techrental.authentication.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByUsername(String username);
    Account findByEmail(String email);
    Account findByPhoneNumber(String phoneNumber);
    Account findByUsernameOrEmail(String username, String email);
    Account findByEmailAndVerificationCode(String email, String verificationCode);


}
