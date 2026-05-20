package com.madan.M360_Task_1.repository;

import com.madan.M360_Task_1.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    List<User> findAllByNameContainingIgnoreCase(String name);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE UPPER(u.name) LIKE UPPER(CONCAT('%', :name, '%'))")
    List<User> findByNameIgnoreCaseFetch(@Param("name") String name);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
