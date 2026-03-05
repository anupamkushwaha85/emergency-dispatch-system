package com.emergency.emergency108.service;

import com.emergency.emergency108.entity.Hospital;
import com.emergency.emergency108.repository.HospitalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for Hospital operations.
 *
 * @author anupam kushwaha
 */
@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    /**
     * Get all hospitals operation.
     * @return the List<Hospital>
     */
    @Transactional(readOnly = true)
    public List<Hospital> getAllHospitals() {
        return hospitalRepository.findAll();
    }

    /**
     * Add hospital operation.
     * @param hospital the hospital
     * @return the Hospital
     */
    @Transactional
    public Hospital addHospital(Hospital hospital) {
        return hospitalRepository.save(hospital);
    }
}
