package com.emergency.emergency108.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {

    public MetricsConfiguration(MeterRegistry registry) {
        // Ensures registry is eagerly initialized
    }
}
