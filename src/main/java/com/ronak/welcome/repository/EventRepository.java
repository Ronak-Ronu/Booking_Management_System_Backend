package com.ronak.welcome.repository;

import com.ronak.welcome.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event,Long> {
}
