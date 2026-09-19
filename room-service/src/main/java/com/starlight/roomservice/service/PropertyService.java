package com.starlight.roomservice.service;

import com.starlight.roomservice.dto.PropertyRequest;
import com.starlight.roomservice.dto.PropertyResponse;
import com.starlight.roomservice.entity.Property;
import com.starlight.roomservice.exception.DuplicateResourceException;
import com.starlight.roomservice.exception.ResourceNotFoundException;
import com.starlight.roomservice.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    public PropertyResponse create(PropertyRequest request) {
        if (propertyRepository.existsByNameIgnoreCaseAndLocationIgnoreCase(request.getName(), request.getLocation())) {
            throw new DuplicateResourceException(
                    "A property named '" + request.getName() + "' already exists at '" + request.getLocation() + "'");
        }

        Property property = new Property();
        property.setName(request.getName());
        property.setLocation(request.getLocation());
        property.setDescription(request.getDescription());
        property.setActive(request.getActive() == null || request.getActive());

        Property saved = propertyRepository.save(property);
        return toResponse(saved);
    }

    public List<PropertyResponse> getAll() {
        return propertyRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PropertyResponse getById(Long id) {
        Property property = findEntityById(id);
        return toResponse(property);
    }

    public PropertyResponse update(Long id, PropertyRequest request) {
        Property property = findEntityById(id);

        property.setName(request.getName());
        property.setLocation(request.getLocation());
        property.setDescription(request.getDescription());
        if (request.getActive() != null) {
            property.setActive(request.getActive());
        }

        Property saved = propertyRepository.save(property);
        return toResponse(saved);
    }

    public void delete(Long id) {
        Property property = findEntityById(id);
        propertyRepository.delete(property);
    }

    private Property findEntityById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
    }

    private PropertyResponse toResponse(Property property) {
        return new PropertyResponse(
                property.getId(),
                property.getName(),
                property.getLocation(),
                property.getDescription(),
                property.isActive()
        );
    }
}
