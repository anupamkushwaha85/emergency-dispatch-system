package com.emergency.emergency108.config;

import com.emergency.emergency108.entity.Ambulance;
import com.emergency.emergency108.entity.AmbulanceStatus;
import com.emergency.emergency108.entity.AmbulanceType;
import com.emergency.emergency108.repository.AmbulanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

@Configuration
public class DatabaseSeeder {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    @Bean
    CommandLineRunner initDatabase(AmbulanceRepository ambulanceRepository, JdbcTemplate jdbcTemplate) {
        return args -> {
            log.info("🚑 Starting Database Seeder...");

            // 1. FIX THE AMBULANCE not found error
            if (ambulanceRepository.existsById(1L)) {
                log.info("✅ Default ambulance (ID 1) found.");
            } else {
                log.info("⚠️ Default ambulance (ID 1) not found. Checking for conflicts...");

                // Cleanup conflicting ambulance data
                jdbcTemplate.update("DELETE FROM ambulances WHERE code = ?", "DL-108-0001");
                jdbcTemplate.update("DELETE FROM ambulances WHERE id = 1");

                log.info("Creating default ambulance with FORCED ID 1...");
                String sql = """
                            INSERT INTO ambulances (
                                id, code, status, last_lat, last_lng, updated_at,
                                ambulance_type, license_plate, version,
                                base_fare, per_km_rate, driver, driver_phone
                            ) VALUES (
                                1, 'DL-108-0001', 'AVAILABLE', 28.6139, 77.2090, NOW(),
                                'GOVERNMENT', 'DL-108-0001', 0,
                                NULL, NULL, NULL, NULL
                            )
                        """;
                jdbcTemplate.update(sql);
                log.info("✅ Created Default Ambulance with FORCED ID 1.");
            }

            // 2. FIX THE "DUPLICATE ENTRY" BUGS (Bad Unique Constraints)

            // Fix 1: Driver Session History
            try {
                log.info("🛠️ Attempting to drop problematic unique index 'uk_active_driver'...");
                jdbcTemplate.execute("DROP INDEX uk_active_driver ON driver_sessions");
                log.info("✅ Successfully dropped 'uk_active_driver'.");
            } catch (Exception e) {
                log.info("ℹ️ Index 'uk_active_driver' likely already dropped.");
            }

            // Fix 2: Ambulance Session History (The new bug)
            try {
                log.info("🛠️ Attempting to drop problematic unique index 'uk_active_ambulance'...");
                jdbcTemplate.execute("DROP INDEX uk_active_ambulance ON driver_sessions");
                log.info("✅ Successfully dropped 'uk_active_ambulance'.");
            } catch (Exception e) {
                log.info("ℹ️ Index 'uk_active_ambulance' likely already dropped.");
            }

            // 3. CLEANUP STUCK SESSIONS
            log.info("🧹 Clearing active driver sessions to unblock login...");
            jdbcTemplate.update("DELETE FROM driver_sessions WHERE status = 'ONLINE'");
            log.info("✅ Active sessions cleared.");

        };
    }
}
