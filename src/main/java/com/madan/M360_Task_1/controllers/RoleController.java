package com.madan.M360_Task_1.controllers;

import com.madan.M360_Task_1.models.Role;
import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @PostMapping()
    public ResponseEntity<?> addRole(@RequestBody Role role){

        Role savedRole = roleService.addRole(role);
        return new ResponseEntity<>(savedRole, HttpStatus.CREATED);

    }

    @GetMapping
    public ResponseEntity<?> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoleById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }



}
