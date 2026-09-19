package com.starlight.roomservice.dto;

import jakarta.validation.constraints.NotBlank;

public class PropertyRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "location is required")
    private String location;

    private String description;

    private Boolean active;

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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
