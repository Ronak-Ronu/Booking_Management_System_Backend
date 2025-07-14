// src/main/java/com/ronak/welcome/entity/Event.java
package com.ronak.welcome.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; // Still useful for event-specific date logic if needed

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true) // Important for Lombok with inheritance
@Table(name = "events") // This table stores event-specific fields and a foreign key to bookable_items
@PrimaryKeyJoinColumn(name = "id") // Maps Event's ID to BookableItem's ID (assuming 'id' is the primary key in BookableItem)
public class Event extends BookableItem { // Extend BookableItem

    // Event-specific fields can go here, for example:
    @Column
    private String eventSpecificField; // Example: e.g., "conference track" or "speaker list"

    // Note: Fields like 'name', 'description', 'location', 'capacity', 'price', 'startTime', 'endTime', 'provider'
    // are now inherited from BookableItem. 'eventDate' is replaced by 'startTime' and 'endTime'.

    // When creating an Event, ensure its 'type' in BookableItem is set to BookableItemType.EVENT
    @PrePersist
    @PreUpdate
    public void setEventType() {
        this.setType(com.ronak.welcome.enums.BookableItemType.EVENT);
    }
}
