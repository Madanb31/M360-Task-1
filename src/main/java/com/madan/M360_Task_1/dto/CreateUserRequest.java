package com.madan.M360_Task_1.dto;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(

        String name,
        String email,
        String contactNum,

        // Address fields
        String street,
        String city,
        String state,
        String zipcode,

        // Role IDs
        Set<UUID> roleIds

) {}