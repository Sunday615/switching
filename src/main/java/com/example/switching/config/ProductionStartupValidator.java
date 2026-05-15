package com.example.switching.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Runs only in the "prod" profile. Validates runtime configuration that cannot be
 * enforced by YAML placeholder syntax alone. Fails startup immediately on any violation
 * so misconfigured deployments are caught before accepting traffic.
 */
@Component
@Profile("prod")
public class ProductionStartupValidator implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ProductionStartupValidator.class);

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${switching.payment.json-initiation.enabled}")
    private boolean jsonInitiationEnabled;

    @Value("${switching.mock-bank.pacs002.force-reject}")
    private boolean mockForceReject;

    @Override
    public void afterPropertiesSet() {
        List<String> violations = new ArrayList<>();

        if (dbUrl.contains("allowPublicKeyRetrieval=true")) {
            violations.add("DB URL must not contain allowPublicKeyRetrieval=true in production. "
                    + "Remove or set allowPublicKeyRetrieval=false.");
        }

        if (dbUrl.contains("localhost") || dbUrl.contains("127.0.0.1")) {
            violations.add("DB URL points to localhost — production must use a remote database host.");
        }

        if (mockForceReject) {
            violations.add("switching.mock-bank.pacs002.force-reject must not be true in production. "
                    + "This is a mock testing flag.");
        }

        if (!violations.isEmpty()) {
            throw new IllegalStateException(
                    "Production startup validation failed with " + violations.size() + " violation(s):\n"
                    + String.join("\n  - ", violations));
        }

        if (jsonInitiationEnabled) {
            log.warn("[PROD] switching.payment.json-initiation.enabled=true — "
                    + "JSON payment path is active. Confirm this is intentional for this deployment.");
        }

        log.info("[PROD] Startup validation passed.");
    }
}
