package com.example.codeeval.security;

import com.example.codeeval.entity.User;
import com.example.codeeval.repository.UserRepository;
import com.example.codeeval.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        logger.debug("JwtAuthenticationFilter: Request URI={}, Authorization header present={}", 
                request.getRequestURI(), authHeader != null);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("JwtAuthenticationFilter: No Bearer token found, skipping");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        logger.debug("JwtAuthenticationFilter: Extracted JWT token");

        try {
            username = jwtUtil.extractUsername(jwt);
            logger.debug("JwtAuthenticationFilter: Extracted username={}", username);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                logger.debug("JwtAuthenticationFilter: Looking up user by username={}", username);
                Optional<User> userOptional = userRepository.findByUsername(username);

                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    logger.debug("JwtAuthenticationFilter: User found, validating token");
                    
                    if (jwtUtil.validateToken(jwt, user)) {
                        logger.debug("JwtAuthenticationFilter: Token validated, setting authentication");
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                user.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        logger.debug("JwtAuthenticationFilter: Authentication set successfully, authorities={}", 
                                user.getAuthorities());
                    } else {
                        logger.debug("JwtAuthenticationFilter: Token validation failed");
                    }
                } else {
                    logger.debug("JwtAuthenticationFilter: User not found");
                }
            } else if (SecurityContextHolder.getContext().getAuthentication() != null) {
                logger.debug("JwtAuthenticationFilter: Authentication already set");
            }
        } catch (Exception e) {
            logger.error("JwtAuthenticationFilter: Error processing token", e);
        }

        filterChain.doFilter(request, response);
    }
}
