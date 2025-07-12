package com.ronak.welcome.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ronak.welcome.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "addresses")
@Data
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String zipCode;

    @Enumerated(EnumType.STRING)
    private AddressType addressType;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private User user;

    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;
}
