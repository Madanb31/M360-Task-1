package com.madan.M360_Task_1.service;

import com.madan.M360_Task_1.dto.CreateUserRequest;
import com.madan.M360_Task_1.dto.UserResponse;
import com.madan.M360_Task_1.models.Address;
import com.madan.M360_Task_1.models.Role;
import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.repository.RoleRepository;
import com.madan.M360_Task_1.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

//    public UserService(UserRepository userRepo) {
//        this.userRepo = userRepo;
//    }


    // Converter: Entity → Response DTO
    public UserResponse toResponse(User user) {

        List<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream()
                .map(Role::getRoleName)
                .toList()
                : List.of();

        List<UUID> roleIds = user.getRoles() != null
                ? user.getRoles().stream()
                .map(Role::getId)
                .toList()
                : List.of();

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getContactNum(),
                user.getAddress() != null ? user.getAddress().getStreet() : null,
                user.getAddress() != null ? user.getAddress().getCity() : null,
                user.getAddress() != null ? user.getAddress().getState() : null,
                user.getAddress() != null ? user.getAddress().getZipCode() : null,
                roleNames,
                roleIds
        );
    }


    public User addUser(CreateUserRequest request) {

        //check user exists by email
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email already exists"
            );
        }

        // Create Address
        Address address = new Address();
        address.setStreet(request.street());
        address.setCity(request.city());
        address.setState(request.state());
        address.setZipCode(request.zipCode());

        // Handle Roles
        Set<Role> roles;

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {

            roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));

            // Validate all role IDs exist
            if (roles.size() != request.roleIds().size()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "One or more roles are invalid"
                );
            }

        } else {

            // Assign default USER role
            Role defaultRole = roleRepository.findByRoleNameIgnoreCase("USER")
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Default USER role not configured"
                            )
                    );

            roles = Set.of(defaultRole);
        }

        //Create User
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setContactNum(request.contactNum());
        user.setAddress(address);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByName(String name) {
        return userRepository.findAllByNameContainingIgnoreCase(name);
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "User not found with id " + id
                        )
                );
    }


    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found with id " + id
            );
        }
        userRepository.deleteById(id);
    }

    public User updateUser(UUID id, CreateUserRequest request) {

        User existingUser = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found with id " + id
                        )
                );

        existingUser.setName(request.name());
        existingUser.setEmail(request.email());
        existingUser.setContactNum(request.contactNum());

        // Update address ONLY if at least one field is provided
        if (request.street() != null || request.city() != null
                || request.state() != null || request.zipCode() != null) {

            Address address = existingUser.getAddress();
            if (address == null) {
                address = new Address();
            }
            // Only update fields that are not null
            if (request.street() != null) address.setStreet(request.street());
            if (request.city() != null) address.setCity(request.city());
            if (request.state() != null) address.setState(request.state());
            if (request.zipCode() != null) address.setZipCode(request.zipCode());
            existingUser.setAddress(address);
        }
        // Update roles if provided
        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
            if (roles.size() != request.roleIds().size()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "One or more roles are invalid"
                );
            }
            existingUser.setRoles(roles);
        }

        return userRepository.save(existingUser);
    }

    public User assignRole(UUID userId, UUID roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found with id " + userId
                        )
                );

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Role not found with id " + roleId
                        )
                );

        user.getRoles().add(role);
        return userRepository.save(user);
    }

    public User removeRole(UUID userId, UUID roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found with id " + userId
                        )
                );

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Role not found with id " + roleId
                        )
                );

        user.getRoles().remove(role);
        return userRepository.save(user);
    }

    // ====== NEW: PAGINATION METHODS ======

    // Simple pagination
    public Page<User> getUsersWithPagination(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable);
    }

    // Pagination + Sorting
    public Page<User> getUsersWithPaginationAndSorting(
            int page, int size, String sortBy, String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findAll(pageable);
    }

}
