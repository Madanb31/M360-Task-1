package com.madan.M360_Task_1.repository;

import com.madan.M360_Task_1.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    boolean existsByRoleName(String roleName);

    Optional<Role> findByRoleNameIgnoreCase(String role);
}
