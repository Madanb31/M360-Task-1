package com.madan.M360_Task_1.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Step 1: Get Authorization header
        String header = request.getHeader("Authorization");

        // Step 2: Check if header exists and starts with "Bearer "
        if (header == null || !header.startsWith("Bearer ")) {
            // No token — continue (might be public API like /auth/login)
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3: Extract token (remove "Bearer " prefix)
        String token = header.substring(7);

        // Step 4: Validate token
        if (jwtUtil.isTokenValid(token)) {

            // Step 5: Extract user info from token
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);

            // Step 6: Create authentication object
            // "ROLE_" prefix is required by Spring Security
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            username,           // principal (who)
                            null,               // credentials (not needed, we have token)
                            List.of(authority)   // authorities (roles)
                    );

            // Step 7: Set in SecurityContext
            // Now Spring knows: "madan is logged in with ADMIN role"
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // Step 8: Continue to next filter / controller
        filterChain.doFilter(request, response);
    }
}
