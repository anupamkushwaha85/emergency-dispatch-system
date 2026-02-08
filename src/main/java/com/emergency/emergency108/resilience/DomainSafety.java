package com.emergency.emergency108.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DomainSafety {

    private static final Logger log =
            LoggerFactory.getLogger(DomainSafety.class);

    private DomainSafety() {}

    public static void runSafely(
            String operation,
            Runnable action
    ) {
        try {
            action.run();
        } catch (Exception ex) {
            log.error(
                    "NON-FATAL failure in {} — continuing",
                    operation,
                    ex
            );
        }
    }
}
