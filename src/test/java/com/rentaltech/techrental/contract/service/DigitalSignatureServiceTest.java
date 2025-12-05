package com.rentaltech.techrental.contract.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class DigitalSignatureServiceTest {

    private final DigitalSignatureService service = new DigitalSignatureService();

    @Test
    void verifySignatureReturnsTrueForSupportedMethod() {
        String signature = Base64.getEncoder().encodeToString("signed".getBytes(StandardCharsets.UTF_8));
        boolean valid = service.verifySignature(signature, "hash", "SMS_OTP");
        assertThat(valid).isTrue();
    }

    @Test
    void verifySignatureReturnsFalseForUnsupportedMethod() {
        String signature = Base64.getEncoder().encodeToString("signed".getBytes(StandardCharsets.UTF_8));
        assertThat(service.verifySignature(signature, "hash", "UNSUPPORTED")).isFalse();
    }

    @Test
    void createTestSignatureProducesBase64Payload() {
        String signature = service.createTestSignature("contract data");
        assertThat(signature).isNotBlank();
        assertThat(signature).matches("[A-Za-z0-9+/=]+");
    }

    @Test
    void verifyContractIntegrityComparesHashes() throws NoSuchAlgorithmException {
        String data = "contract";
        String hash = sha256(data);
        assertThat(service.verifyContractIntegrity(data, hash)).isTrue();
        assertThat(service.verifyContractIntegrity(data + "!", hash)).isFalse();
    }

    @Test
    void timestampHelpersValidateAge() {
        long timestamp = service.generateSignatureTimestamp();
        assertThat(service.validateSignatureTimestamp(timestamp, 1)).isTrue();
        long oldTimestamp = timestamp - (2 * 60 * 1000);
        assertThat(service.validateSignatureTimestamp(oldTimestamp, 1)).isFalse();
    }

    private String sha256(String payload) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

}
