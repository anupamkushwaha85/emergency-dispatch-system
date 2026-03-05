package com.emergency.emergency108.auth.dto;

/**
 * Represents the ProfileUpdateRequest component in the system.
 *
 * @author anupam kushwaha
 */
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

    /**
     * Set name operation.
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get address operation.
     * @return the String
     */
    public String getAddress() {
        return address;
    }

    /**
     * Set address operation.
     * @param address the address
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Get email operation.
     * @return the String
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set email operation.
     * @param email the email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Get language operation.
     * @return the String
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Set language operation.
     * @param language the language
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Get gender operation.
     * @return the String
     */
    public String getGender() {
        return gender;
    }

    /**
     * Set gender operation.
     * @param gender the gender
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * Get date of birth operation.
     * @return the String
     */
    public String getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Set date of birth operation.
     * @param dateOfBirth the dateOfBirth
     */
    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * Get age operation.
     * @return the Integer
     */
    public Integer getAge() {
        return age;
    }

    /**
     * Set age operation.
     * @param age the age
     */
    public void setAge(Integer age) {
        this.age = age;
    }

    /**
     * Get blood group operation.
     * @return the String
     */
    public String getBloodGroup() {
        return bloodGroup;
    }

    /**
     * Set blood group operation.
     * @param bloodGroup the bloodGroup
     */
    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }
}
