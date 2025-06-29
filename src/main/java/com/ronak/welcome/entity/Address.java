package com.ronak.welcome.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ronak.welcome.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    @Enumerated(EnumType.STRING)
    private AddressType addressType;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private User user;

}