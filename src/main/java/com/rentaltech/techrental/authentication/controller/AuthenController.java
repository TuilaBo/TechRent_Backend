package com.rentaltech.techrental.authentication.controller;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.dto.AccountCreateRequest;
import com.rentaltech.techrental.authentication.model.dto.CreateUserRequestDto;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.model.dto.JWTAuthResponse;
import com.rentaltech.techrental.authentication.model.dto.LoginDto;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthenController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AccountService accountService;

    @PostMapping("/login")
    public ResponseEntity<JWTAuthResponse> authenticateUser(
            @RequestBody @Valid LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsernameOrEmail(),
                        loginDto.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        return ResponseEntity.ok(new JWTAuthResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(
            @RequestBody @Valid CreateUserRequestDto request) {
        Account account = Account.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .isActive(false)
                .role(Role.User)
                .build();
        Account saved = accountService.addAccount(account);
        accountService.setVerificationCodeAndSendEmail(saved);
        return new ResponseEntity<>("User registered successfully! Please verify your email.", HttpStatus.CREATED);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(
            @RequestParam("email") String email,
            @RequestParam("code") String code) {
        boolean ok = accountService.verifyEmail(email, code);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired verification code");
        }
        return ResponseEntity.ok("Email verified successfully. Your account is now active.");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = principal.getUsername();
        return accountService.getByUsername(username)
                .<ResponseEntity<?>>map(a -> ResponseEntity.ok(
                        com.rentaltech.techrental.authentication.model.dto.AccountMeResponse.builder()
                                .accountId(a.getAccountId())
                                .username(a.getUsername())
                                .email(a.getEmail())
                                .role(a.getRole())
                                .phoneNumber(a.getPhoneNumber())
                                .isActive(a.getIsActive())
                                .build()
                ))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}