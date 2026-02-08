package com.hackathon.emergency108.auth.dto;

public class ProfileUpdateRequest {
    private String name;
    private String address;
    private String email;
    private String language;
    private String gender;
    private String dateOfBirth; // Format: yyyy-MM-dd
    private Integer age;
    private String bloodGroup;

    // Constructors
    public ProfileUpdateRequest() {
    }

    public ProfileUpdateRequest(String name, String address, String email, String language,
            String gender, String dateOfBirth, Integer age, String bloodGroup) {
        this.name = name;
        this.address = address;
        this.email = email;
        this.language = language;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.age = age;
        this.bloodGroup = bloodGroup;
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }
}
