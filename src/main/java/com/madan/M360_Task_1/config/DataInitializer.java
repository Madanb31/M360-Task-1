package com.madan.M360_Task_1.config;

import com.madan.M360_Task_1.models.Role;
import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.repository.RoleRepository;
import com.madan.M360_Task_1.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // Create default roles if they don't exist
        Role adminRole = createRoleIfNotExists("ADMIN");
        Role userRole = createRoleIfNotExists("USER");

        // Create default admin user if doesn't exist
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setName("Admin");
            admin.setEmail("admin@example.com");
            admin.setContactNum("0000000000");
            admin.setRoles(Set.of(adminRole));

            userRepository.save(admin);
            System.out.println("✅ Default admin user created (username: admin, password: admin123)");
        }

        System.out.println("✅ Default roles ready: ADMIN, USER");
    }

    private Role createRoleIfNotExists(String roleName) {
        return roleRepository.findByRoleNameIgnoreCase(roleName)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRoleName(roleName);
                    return roleRepository.save(role);
                });
    }
}