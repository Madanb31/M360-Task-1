package com.madan.M360_Task_1.service;

import com.madan.M360_Task_1.models.Role;
import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    public Role addRole(Role role) {

        if (roleRepository.existsByRoleName(role.getRoleName())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role already exists with name: " + role.getRoleName()
            );
        }


        return roleRepository.save(role);

    }

    public List<Role> getAllRoles() {

        return roleRepository.findAll();

    }


    public Object getRoleById(UUID id ) {
        return roleRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Role not found with id " + id
                        )
                );
    }
}
