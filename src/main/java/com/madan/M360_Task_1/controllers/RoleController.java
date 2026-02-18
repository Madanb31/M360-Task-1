package com.madan.M360_Task_1.controllers;

import com.madan.M360_Task_1.models.Role;
import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Role management APIs")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Operation(summary = "Create role", description = "Create a new role (ADMIN only)")
    @PostMapping()
    public ResponseEntity<?> addRole(@Valid @RequestBody Role role){

        Role savedRole = roleService.addRole(role);
        return new ResponseEntity<>(savedRole, HttpStatus.CREATED);

    }

    @Operation(summary = "Get all roles", description = "Get list of all roles")
    @GetMapping
    public ResponseEntity<?> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @Operation(summary = "Get role by ID", description = "Get role details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> getRoleById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }


}
