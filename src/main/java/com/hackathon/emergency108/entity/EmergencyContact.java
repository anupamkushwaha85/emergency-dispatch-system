package com.hackathon.emergency108.entity;

import jakarta.persistence.*;

/**
 * Emergency contact information for users.
 * Users can add family/friends to notify in case of emergency.
 */
@Entity
@Table(name = "emergency_contacts")
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who owns this emergency contact
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Contact person's name
     */
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /**
     * Contact person's phone number
     */
    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    /**
     * Relationship: SPOUSE, PARENT, CHILD, SIBLING, FRIEND, etc.
     */
    @Column(name = "relation", length = 50)
    private String relation;

    // Constructors
    public EmergencyContact() {
    }

    public EmergencyContact(Long userId, String name, String phone, String relation) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.relation = relation;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }
}
