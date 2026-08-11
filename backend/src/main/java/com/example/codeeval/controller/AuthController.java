package com.example.codeeval.controller;

import com.example.codeeval.dto.ApiResponse;
import com.example.codeeval.dto.LoginRequest;
import com.example.codeeval.dto.LoginResponse;
import com.example.codeeval.dto.RegisterRequest;
import com.example.codeeval.dto.UserDTO;
import com.example.codeeval.entity.User;
import com.example.codeeval.repository.RefreshTokenRepository;
import com.example.codeeval.repository.UserRepository;
import com.example.codeeval.service.UserService;
import com.example.codeeval.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public AuthController(UserService userService, JwtUtil jwtUtil, 
                          RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> register(@Valid @RequestBody RegisterRequest request) {
        UserDTO user = userService.register(request);
        return ResponseEntity.ok(ApiResponse.success("注册成功", user));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                             HttpServletResponse response) {
        LoginResponse loginResponse = userService.login(request);

        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        
        String refreshToken = jwtUtil.generateRefreshToken(user);
        
        com.example.codeeval.entity.RefreshToken tokenEntity = 
                new com.example.codeeval.entity.RefreshToken(refreshToken, user, 
                        LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(tokenEntity);

        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(refreshTokenCookie);
        String cookieHeader = response.getHeader("Set-Cookie");
        if (cookieHeader != null) {
            response.setHeader("Set-Cookie", cookieHeader + "; SameSite=Strict");
        }

        return ResponseEntity.ok(ApiResponse.success("登录成功", loginResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null || !jwtUtil.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(401).body(ApiResponse.error("无效的刷新令牌"));
        }

        var tokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElse(null);

        if (tokenEntity == null || tokenEntity.isExpired()) {
            return ResponseEntity.status(401).body(ApiResponse.error("刷新令牌已过期"));
        }

        User user = tokenEntity.getUser();
        String newAccessToken = jwtUtil.generateToken(user);
        
        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(3600000L)
                .user(UserDTO.fromEntity(user))
                .build();

        return ResponseEntity.ok(ApiResponse.success("Token刷新成功", loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent(refreshTokenRepository::delete);
        }

        Cookie refreshTokenCookie = new Cookie("refresh_token", "");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        response.addCookie(refreshTokenCookie);
        String cookieHeader = response.getHeader("Set-Cookie");
        if (cookieHeader != null) {
            response.setHeader("Set-Cookie", cookieHeader + "; SameSite=Strict");
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(ApiResponse.success("退出登录成功", null));
    }
}
