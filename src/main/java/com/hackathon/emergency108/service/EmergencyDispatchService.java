package com.hackathon.emergency108.service;

import com.hackathon.emergency108.entity.Ambulance;
import com.hackathon.emergency108.entity.AmbulanceStatus;
import com.hackathon.emergency108.event.AssignmentEvent;
import com.hackathon.emergency108.event.DomainEventPublisher;
import com.hackathon.emergency108.repository.AmbulanceRepository;
import com.hackathon.emergency108.util.GeoUtil;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class EmergencyDispatchService {

    private final AmbulanceRepository ambulanceRepository;
    private final DomainEventPublisher eventPublisher;

    public EmergencyDispatchService(
            AmbulanceRepository ambulanceRepository,
            DomainEventPublisher eventPublisher
    ) {
        this.ambulanceRepository = ambulanceRepository;
        this.eventPublisher = eventPublisher;
    }

    public Ambulance assignNearestAmbulance(double lat, double lng) {

        List<Ambulance> available =
                ambulanceRepository.findAvailableForUpdate();

        if (available.isEmpty()) {
            throw new RuntimeException("No ambulances available");
        }

        Ambulance chosen =
                available.stream()
                        .min(Comparator.comparingDouble(a ->
                                GeoUtil.distanceKm(
                                        lat,
                                        lng,
                                        a.getLatitude(),
                                        a.getLongitude()
                                )
                        ))
                        .orElseThrow();

        // 🔒 LOCK IT FOR REAL
        chosen.setStatus(AmbulanceStatus.BUSY);
        ambulanceRepository.save(chosen);

        // 📣 EVENT AFTER STATE CHANGE
        eventPublisher.publish(
                new AssignmentEvent(
                        null,
                        chosen.getId(),
                        "AMBULANCE_LOCKED",
                        "Ambulance locked for dispatch"
                )
        );

        return chosen;
    }

    public Ambulance assignNearestAmbulanceExcluding(
            double lat,
            double lng,
            List<Long> excludedIds
    ) {

        List<Ambulance> available =
                ambulanceRepository.findAvailableForUpdate();

        available.removeIf(a -> excludedIds.contains(a.getId()));

        if (available.isEmpty()) {
            throw new RuntimeException("No new ambulances available");
        }

        Ambulance chosen =
                available.stream()
                        .min(Comparator.comparingDouble(a ->
                                GeoUtil.distanceKm(
                                        lat,
                                        lng,
                                        a.getLatitude(),
                                        a.getLongitude()
                                )
                        ))
                        .orElseThrow();

        // 🔒 LOCK IT FOR REAL
        chosen.setStatus(AmbulanceStatus.BUSY);
        ambulanceRepository.save(chosen);

        // 📣 EVENT AFTER STATE CHANGE
        eventPublisher.publish(
                new AssignmentEvent(
                        null,
                        chosen.getId(),
                        "AMBULANCE_LOCKED",
                        "Ambulance locked for dispatch"
                )
        );

        return chosen;
    }
}


