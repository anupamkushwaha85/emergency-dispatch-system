package com.hackathon.emergency108.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DomainEventLogger {

    private static final Logger log =
            LoggerFactory.getLogger(DomainEventLogger.class);

    @EventListener
    public void onDomainEvent(DomainEvent event) {

        log.info(
                "DOMAIN_EVENT type={} time={} details={}",
                event.type(),
                event.occurredAt(),
                event.description()
        );
    }
}
