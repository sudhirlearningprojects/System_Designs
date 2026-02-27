package org.sudhir512kj.netflix.dto;

import java.util.List;

public class UserRegistrationRequest {
    private String email;
    private String password;
    private String name;
    private String region;
    private List<String> preferredGenres;
    
    public UserRegistrationRequest() {}
    
    public UserRegistrationRequest(String email, String password, String name, String region) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.region = region;
    }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    
    public List<String> getPreferredGenres() { return preferredGenres; }
    public void setPreferredGenres(List<String> preferredGenres) { this.preferredGenres = preferredGenres; }
}