package com.hackathon.emergency108.auth.dto;

public class ProfileUpdateRequest {
    private String name;
    private String address;
    private String email;
    private String language;

    // Constructors
    public ProfileUpdateRequest() {}

    public ProfileUpdateRequest(String name, String address, String email, String language) {
        this.name = name;
        this.address = address;
        this.email = email;
        this.language = language;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
