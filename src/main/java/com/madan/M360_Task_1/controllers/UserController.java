package com.madan.M360_Task_1.controllers;

import com.madan.M360_Task_1.dto.CreateUserRequest;
import com.madan.M360_Task_1.dto.UserResponse;
import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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


}
