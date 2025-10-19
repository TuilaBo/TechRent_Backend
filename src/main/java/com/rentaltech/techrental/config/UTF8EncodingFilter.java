package com.rentaltech.techrental.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(1)
public class UTF8EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Set UTF-8 encoding for request
        if (httpRequest.getCharacterEncoding() == null) {
            httpRequest.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }
        
        // Set UTF-8 encoding for response
        httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        httpResponse.setContentType("application/json; charset=UTF-8");
        
        // Add UTF-8 headers
        httpResponse.setHeader("Content-Type", "application/json; charset=UTF-8");
        httpResponse.setHeader("Accept-Charset", "UTF-8");
        
        chain.doFilter(request, response);
    }
}
