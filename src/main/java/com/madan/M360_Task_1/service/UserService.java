package com.madan.M360_Task_1.service;

import com.madan.M360_Task_1.dto.UserResponse;
import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }


    //Dto helper
//    public UserResponse toUserDto(User user){
//        return new UserResponse(
//                user.getName(),
//                user.getEmail()
//        );
//    }


    public User addUser(User user) {
        return userRepo.save(user);
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public List<User> getUsersByName(String name) {
        return userRepo.findAllByNameContainingIgnoreCase(name);
    }

    public User getUserById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "User not found with id " + id
                        )
                );
    }


    public void deleteUser(Long id) {
        if (!userRepo.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found with id " + id
            );
        }
        userRepo.deleteById(id);
    }

    public User updateUser(Long id, User user) {

        User existingUser = userRepo.findById(id)
                .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "User not found with id " + id
                            )
                );

        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setContactNum(user.getContactNum());


        return userRepo.save(existingUser);

    }
}
