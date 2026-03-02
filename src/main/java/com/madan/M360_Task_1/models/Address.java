package com.madan.M360_Task_1.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue
    private UUID id;

    private String street;

    private String city;

    private String state;

    @Column(name = "zipcode")
    private String zipCode;

}
