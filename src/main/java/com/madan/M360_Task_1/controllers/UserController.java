package com.madan.M360_Task_1.controllers;

import com.madan.M360_Task_1.dto.CreateUserRequest;
import com.madan.M360_Task_1.dto.UserResponse;
import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.service.UserService;
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
    public ResponseEntity<?> addUser(@RequestBody CreateUserRequest request){

        User savedUser = userService.addUser(request);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);

    }

    @GetMapping()
    public ResponseEntity<List<User>> getAllUsers(){
        return new ResponseEntity<>(userService.getAllUsers(), HttpStatus.OK);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<List<User>> getUsersByName(
            @PathVariable String name
    ){
        return new ResponseEntity<>(userService.getUsersByName(name), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(
            @PathVariable UUID id
    ){
        return new ResponseEntity<>(userService.getUserById(id), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id,
                                        @RequestBody CreateUserRequest request) {
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
