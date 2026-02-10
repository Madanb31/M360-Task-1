package com.madan.M360_Task_1.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class User {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @Column(nullable = false, unique = true)
    private String email;


    private String contactNum;

}
