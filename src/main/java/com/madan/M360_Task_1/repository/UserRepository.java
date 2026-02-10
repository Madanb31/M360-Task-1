package com.madan.M360_Task_1.repository;

import com.madan.M360_Task_1.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findAllByNameContainingIgnoreCase(String name);

}
