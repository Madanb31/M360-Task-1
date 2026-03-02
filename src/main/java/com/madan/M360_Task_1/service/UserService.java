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
import org.springframework.security.crypto.password.PasswordEncoder;
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
    @Autowired
    private PasswordEncoder passwordEncoder;

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
                user.getUsername(),
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

    // 1. Create User with specific role (for UserCreateTools)
    public User createUserWithRole(String username, String name, String email, String contact, String roleName) {

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setName(name);
        user.setEmail(email);
        user.setContactNum(contact);
        user.setPassword(passwordEncoder.encode("default123")); // Default password

        String targetRole = (roleName == null || roleName.isBlank()) ? "USER" : roleName.trim().toUpperCase();
        Role role = roleRepository.findByRoleNameIgnoreCase(targetRole)
                .orElseThrow(() -> new RuntimeException("Role not found: " + targetRole));

        user.setRoles(Set.of(role));

        return userRepository.save(user);
    }

    // 2. Add Role (for ActionRequestService)
    public void addRoleToUser(UUID userId, String roleName) {
        User user = getUserById(userId); // Reuses existing method
        Role role = roleRepository.findByRoleNameIgnoreCase(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        user.getRoles().add(role);
        userRepository.save(user);
    }

    // 3. Remove Role (for ActionRequestService)
    public void removeRoleFromUser(UUID userId, String roleName) {
        User user = getUserById(userId);

        // Safe remove using name (avoids proxy issues)
        boolean removed = user.getRoles().removeIf(r -> r.getRoleName().equalsIgnoreCase(roleName));

        if (!removed) {
            throw new RuntimeException("User does not have role: " + roleName);
        }
        userRepository.save(user);
    }


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
