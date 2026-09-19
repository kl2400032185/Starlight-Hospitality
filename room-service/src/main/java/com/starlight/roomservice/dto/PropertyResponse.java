package com.starlight.roomservice.dto;

public class PropertyResponse {

    private Long id;
    private String name;
    private String location;
    private String description;
    private boolean active;

    public PropertyResponse() {
    }

    public PropertyResponse(Long id, String name, String location, String description, boolean active) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
