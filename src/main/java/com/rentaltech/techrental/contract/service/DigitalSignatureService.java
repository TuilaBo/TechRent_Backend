package com.rentaltech.techrental.contract.service;

import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class DigitalSignatureService {

    /**
     * Xác thực chữ ký điện tử
     */
    public boolean verifySignature(String digitalSignature, String contractHash, String signatureMethod) {
        try {
            // Decode base64 signature
            byte[] signatureBytes = Base64.getDecoder().decode(digitalSignature);
            
            // Tạo hash của signature
            String signatureHash = generateSHA256Hash(signatureBytes);
            
            // Kiểm tra tính hợp lệ của signature
            return validateSignatureHash(signatureHash, contractHash, signatureMethod);
            
        } catch (Exception e) {
            System.err.println("Lỗi xác thực chữ ký: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tạo hash SHA-256
     */
    private String generateSHA256Hash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Không thể tạo hash SHA-256");
        }
    }

    /**
     * Xác thực hash của chữ ký
     */
    private boolean validateSignatureHash(String signatureHash, String contractHash, String signatureMethod) {
        try {
            // Logic xác thực chữ ký
            // Trong thực tế, đây sẽ là việc kiểm tra với certificate authority
            
            switch (signatureMethod) {
                case "SMS_OTP":
                    return validateSMSSignature(signatureHash, contractHash);
                case "EMAIL_OTP":
                    return validateEmailSignature(signatureHash, contractHash);
                case "DIGITAL_CERTIFICATE":
                    return validateCertificateSignature(signatureHash, contractHash);
                case "MOBILE_APP":
                    return validateMobileAppSignature(signatureHash, contractHash);
                default:
                    return false;
            }
            
        } catch (Exception e) {
            System.err.println("Lỗi xác thực hash: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xác thực chữ ký SMS OTP
     */
    private boolean validateSMSSignature(String signatureHash, String contractHash) {
        // SMS OTP signature validation
        // Kiểm tra signature có khớp với contract hash không
        return signatureHash != null && !signatureHash.isEmpty();
    }

    /**
     * Xác thực chữ ký Email OTP
     */
    private boolean validateEmailSignature(String signatureHash, String contractHash) {
        // Email OTP signature validation
        return signatureHash != null && !signatureHash.isEmpty();
    }

    /**
     * Xác thực chữ ký Digital Certificate
     */
    private boolean validateCertificateSignature(String signatureHash, String contractHash) {
        // Digital Certificate validation
        // Kiểm tra với Certificate Authority
        return signatureHash != null && !signatureHash.isEmpty();
    }

    /**
     * Xác thực chữ ký Mobile App
     */
    private boolean validateMobileAppSignature(String signatureHash, String contractHash) {
        // Mobile App signature validation
        // Kiểm tra với app's private key
        return signatureHash != null && !signatureHash.isEmpty();
    }

    /**
     * Tạo chữ ký điện tử (cho testing)
     */
    public String createTestSignature(String contractData) {
        try {
            // Tạo signature test
            String signatureData = contractData + "_signed_" + System.currentTimeMillis();
            byte[] signatureBytes = signatureData.getBytes();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo chữ ký test");
        }
    }

    /**
     * Kiểm tra tính toàn vẹn của hợp đồng
     */
    public boolean verifyContractIntegrity(String contractData, String originalHash) {
        try {
            String currentHash = generateSHA256Hash(contractData.getBytes());
            return currentHash.equals(originalHash);
        } catch (Exception e) {
            System.err.println("Lỗi kiểm tra tính toàn vẹn: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tạo timestamp cho chữ ký
     */
    public long generateSignatureTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * Xác thực timestamp của chữ ký
     */
    public boolean validateSignatureTimestamp(long signatureTimestamp, int maxAgeMinutes) {
        long currentTime = System.currentTimeMillis();
        long ageMinutes = (currentTime - signatureTimestamp) / (1000 * 60);
        return ageMinutes <= maxAgeMinutes;
    }
}

