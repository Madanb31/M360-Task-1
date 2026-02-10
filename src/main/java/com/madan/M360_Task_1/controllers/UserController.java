package com.madan.M360_Task_1.controllers;

import com.madan.M360_Task_1.dto.UserResponse;
import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping()
    public ResponseEntity<?> addUser(@RequestBody User user){

        User savedUser = userService.addUser(user);
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
            @PathVariable Long id
    ){
        return new ResponseEntity<>(userService.getUserById(id), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User user
    ){
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable Long id
    ){
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }


}
