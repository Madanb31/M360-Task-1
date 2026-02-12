package com.madan.M360_Task_1.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Address {

    @Id
    @GeneratedValue
    private UUID id;

    private String street;

    private String city;

    private String state;

    private String zipcode;

}
