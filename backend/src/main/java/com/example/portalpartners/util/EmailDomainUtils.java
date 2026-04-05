package com.example.portalpartners.util;

import com.example.portalpartners.exceptions.BusinessRulesException;

public final class EmailDomainUtils {
    private EmailDomainUtils() {
    }

    public static String extractDomain(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new BusinessRulesException("E-mail invalido para resolver dominio da organizacao");
        }

        String[] parts = email.trim().toLowerCase().split("@");
        if (parts.length != 2 || parts[1].isBlank()) {
            throw new BusinessRulesException("E-mail invalido para resolver dominio da organizacao");
        }

        return parts[1];
    }

    public static String asDomainSuffix(String email) {
        return "@" + extractDomain(email);
    }
}
