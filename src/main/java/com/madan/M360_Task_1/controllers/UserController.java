package com.madan.M360_Task_1.controllers;

import com.madan.M360_Task_1.dto.CreateUserRequest;
import com.madan.M360_Task_1.dto.UserResponse;
import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

//    public UserController(UserService userService) {
//        this.userService = userService;
//    }

    @PostMapping()
    public ResponseEntity<?> addUser(@Valid @RequestBody CreateUserRequest request){

        User savedUser = userService.addUser(request);
        return new ResponseEntity<>(userService.toResponse(savedUser), HttpStatus.CREATED);

    }

    @GetMapping()
    public ResponseEntity<?> getAllUsers(){
        List<UserResponse> users = userService.getAllUsers()
                .stream()
                .map(userService::toResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<List<User>> getUsersByName(
            @PathVariable String name
    ){
        return new ResponseEntity<>(userService.getUsersByName(name), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable UUID id
    ){
        User user = userService.getUserById(id);
        return ResponseEntity.ok(userService.toResponse(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id,
                                        @Valid @RequestBody CreateUserRequest request) {
        User updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<?> assignRole(@PathVariable UUID userId,
                                        @PathVariable UUID roleId) {
        User user = userService.assignRole(userId, roleId);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<?> removeRole(@PathVariable UUID userId,
                                        @PathVariable UUID roleId) {
        User user = userService.removeRole(userId, roleId);
        return ResponseEntity.ok(user);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable UUID id
    ){
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }


    // ====== NEW PAGINATION ENDPOINTS ======

    // Simple pagination
    // GET /users/page?page=0&size=10
    @GetMapping("/page")
    public ResponseEntity<?> getUsersWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<User> usersPage = userService.getUsersWithPagination(page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("users", usersPage.getContent()
                .stream()
                .map(userService::toResponse)
                .toList());
        response.put("currentPage", usersPage.getNumber());
        response.put("totalItems", usersPage.getTotalElements());
        response.put("totalPages", usersPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    // Pagination + Sorting
    // GET /users/page/sort?page=0&size=10&sortBy=name&direction=asc
    @GetMapping("/page/sort")
    public ResponseEntity<?> getUsersWithPaginationAndSorting(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Page<User> usersPage = userService.getUsersWithPaginationAndSorting(
                page, size, sortBy, direction);

        Map<String, Object> response = new HashMap<>();
        response.put("users", usersPage.getContent()
                .stream()
                .map(userService::toResponse)
                .toList());
        response.put("currentPage", usersPage.getNumber());
        response.put("totalItems", usersPage.getTotalElements());
        response.put("totalPages", usersPage.getTotalPages());
        response.put("sortBy", sortBy);
        response.put("direction", direction);

        return ResponseEntity.ok(response);
    }

}
