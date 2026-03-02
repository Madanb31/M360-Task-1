package com.madan.M360_Task_1.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = "users")
@EqualsAndHashCode(exclude = "users")
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank(message = "Role name is required")
    @Column(nullable = false, unique = true)
    private String roleName;


    @ManyToMany(mappedBy = "roles")
    @JsonIgnore
    private Set<User> users;
}