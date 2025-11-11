package com.rentaltech.techrental.finance.util;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Test utility to verify VNPAY hash generation
 * Use this to compare with VNPAY's expected hash
 */
@Slf4j
public class VnpayHashTest {

    public static void main(String[] args) {
        // Test with known values matching your current setup
        Map<String, String> testParams = new HashMap<>();
        testParams.put("vnp_Version", "2.1.0");
        testParams.put("vnp_Command", "pay");
        testParams.put("vnp_TmnCode", "MCOOOKCG");
        testParams.put("vnp_Amount", "1520000000");
        testParams.put("vnp_CurrCode", "VND");
        testParams.put("vnp_TxnRef", "1762880206997");
        testParams.put("vnp_OrderInfo", "Thanhtoandonhang102");
        testParams.put("vnp_OrderType", "other");
        testParams.put("vnp_Locale", "vn");
        testParams.put("vnp_ReturnUrl", "https://haunched-karina-nondiscriminatively.ngrok-free.dev/api/v1/vnpay/ipn");
        testParams.put("vnp_IpAddr", "127.0.0.1");
        testParams.put("vnp_CreateDate", "20251111235647");

        String secretKey = "7P3XI2WYW9U345M89WWNV2EWHCED4WXJ";
        
        String hash = VnpayUtil.hashAllFields(testParams, secretKey);
        System.out.println("Test hash: " + hash);
        System.out.println("Secret key: " + secretKey);
        System.out.println("Secret key length: " + secretKey.length());
    }
}

