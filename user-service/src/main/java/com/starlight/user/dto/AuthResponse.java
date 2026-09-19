package com.starlight.user.dto;

public class AuthResponse {

    private String token;
    private String tokenType;

    private Long userId;
    private String name;
    private String email;
    private String role;

    public AuthResponse() {
    }

    public AuthResponse(
            String token,
            Long userId,
            String name,
            String email,
            String role
    ) {
        this.token = token;
        this.tokenType = "Bearer";
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}