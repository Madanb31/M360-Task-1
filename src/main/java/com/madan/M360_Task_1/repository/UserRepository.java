package com.madan.M360_Task_1.repository;

import com.madan.M360_Task_1.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    List<User> findAllByNameContainingIgnoreCase(String name);

}
