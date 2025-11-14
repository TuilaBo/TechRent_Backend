package com.rentaltech.techrental.finance.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class VnpayUtil {

    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] hash = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating HMAC SHA512", e);
            return null;
        }
    }

    public static String hashAllFields(Map<String, String> fields, String secretKey) {
        return hashAllFields(fields, secretKey, true);
    }
    
    public static String hashAllFields(Map<String, String> fields, String secretKey, boolean encodeValues) {
        // Remove secure hash fields before processing
        Map<String, String> fieldsToHash = new HashMap<>(fields);
        fieldsToHash.remove("vnp_SecureHash");
        fieldsToHash.remove("vnp_SecureHashType");
        
        // Sort fields by key (alphabetical order)
        List<String> sortedKeys = new ArrayList<>(fieldsToHash.keySet());
        Collections.sort(sortedKeys);
        
        // Build query string - VNPAY format: key1=value1&key2=value2&...
        StringBuilder queryString = new StringBuilder();
        boolean first = true;
        for (String key : sortedKeys) {
            String value = fieldsToHash.get(key);
            // Only include non-null and non-empty values
            if (value != null && !value.isEmpty()) {
                if (!first) {
                    queryString.append("&");
                }
                // When creating payment URL, encode values. When validating callback, use raw values (already decoded by servlet)
                String finalValue = encodeValues 
                    ? URLEncoder.encode(value, StandardCharsets.UTF_8).replace("%20", "+")
                    : value;
                queryString.append(key).append("=").append(finalValue);
                first = false;
            }
        }
        
        String query = queryString.toString();
        log.info("VNPAY hash query string: {}", query);
        log.info("VNPAY hash secret: {}", secretKey);
        log.info("VNPAY hash secret length: {}", secretKey != null ? secretKey.length() : 0);
        log.info("VNPAY encode values: {}", encodeValues);
        
        String hash = hmacSHA512(secretKey, query);
        log.info("VNPAY generated hash: {}", hash);
        return hash;
    }

    public static String getPaymentUrl(Map<String, String> params, String baseUrl) {
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length() > 0) {
                queryString.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                queryString.append("=");
                queryString.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                queryString.append("&");
            }
        }
        if (queryString.length() > 0) {
            queryString.deleteCharAt(queryString.length() - 1);
        }
        return baseUrl + "?" + queryString;
    }

    public static Map<String, String> getRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String paramValue = request.getParameter(paramName);
            params.put(paramName, paramValue);
        }
        return params;
    }
}

