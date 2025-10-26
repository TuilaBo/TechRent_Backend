package com.rentaltech.techrental.config;

import com.rentaltech.techrental.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityWebConfig {

    private static final String[] SECURED_ENDPOINTS = {
            "/api/device-categories/**",
            "/api/devices/**",
            "/api/device-models/**",
            "/api/staff/**",
            "/api/staff/task-categories/**",
            "/api/staff/tasks/**",
            "/api/customer/**",
            "/api/rental-orders/**",
            "/api/operator/tasks/**",
            "/api/technician/tasks/**",
            "/api/contracts/**",
            "/api/accessories/**",
            "/api/accessory-categories",
            "/api/admin/**"
    };


    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // thêm vào SecurityConfig
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, SECURED_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.POST, SECURED_ENDPOINTS).authenticated()
                        .requestMatchers(HttpMethod.PUT, SECURED_ENDPOINTS).authenticated()
                        .requestMatchers(HttpMethod.DELETE, SECURED_ENDPOINTS).authenticated()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().permitAll()
                )
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    // Use NoOp encoder so Spring accepts raw passwords (no {id} prefix required)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

}

