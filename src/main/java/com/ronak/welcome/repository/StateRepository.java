package com.ronak.welcome.repository;

import com.ronak.welcome.entity.Country;
import com.ronak.welcome.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StateRepository extends JpaRepository<State, Long> {
    Optional<State> findByNameIgnoreCaseAndCountry(String name, Country country);
}
