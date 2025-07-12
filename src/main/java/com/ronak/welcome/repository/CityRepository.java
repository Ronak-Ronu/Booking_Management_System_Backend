package com.ronak.welcome.repository;

import com.ronak.welcome.entity.City;
import com.ronak.welcome.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {
    Optional<City> findByNameIgnoreCaseAndState(String name, State state);
}
