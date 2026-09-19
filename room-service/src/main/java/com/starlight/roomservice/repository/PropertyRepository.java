package com.starlight.roomservice.repository;

import com.starlight.roomservice.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    Optional<Property> findByNameIgnoreCaseAndLocationIgnoreCase(String name, String location);

    boolean existsByNameIgnoreCaseAndLocationIgnoreCase(String name, String location);
}
