package com.example.codeeval.service;

import com.example.codeeval.dto.LoginRequest;
import com.example.codeeval.dto.LoginResponse;
import com.example.codeeval.dto.RegisterRequest;
import com.example.codeeval.dto.UserDTO;
import com.example.codeeval.entity.User;
import com.example.codeeval.repository.UserRepository;
import com.example.codeeval.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务类
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public UserDTO register(RegisterRequest request) {
        // 公开注册入口：系统已改为仅管理员创建账号，此处作为兜底仍强制 STUDENT
        return createUserInternal(request, User.Role.STUDENT, false);
    }

    /**
     * 管理员后台创建用户入口：允许指定任意角色（STUDENT / TEACHER / ADMIN）
     * 与公开注册 register() 分离，两条独立业务链路，避免互相污染权限策略
     */
    public UserDTO createUserByAdmin(RegisterRequest request) {
        User.Role targetRole;
        try {
            targetRole = User.Role.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的角色: " + request.getRole());
        }
        return createUserInternal(request, targetRole, true);
    }

    /**
     * 创建用户的内部公共方法
     * @param request 用户信息
     * @param forcedRole 若不为 null，则强制使用此角色（忽略 request 中的 role）
     * @param allowCustomRole true=管理员创建(允许自定义角色)，false=公开注册(强制 STUDENT)
     */
    private UserDTO createUserInternal(RegisterRequest request, User.Role forcedRole, boolean allowCustomRole) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("邮箱已被注册");
        }

        User.Role role;
        if (allowCustomRole && forcedRole != null) {
            // 管理员创建：使用传入的角色（已在上层校验）
            role = forcedRole;
        } else {
            // 公开注册：无论客户端传什么，都强制 STUDENT
            role = User.Role.STUDENT;
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .name(request.getName())
                .role(role)
                .enabled(true)
                .build();

        userRepository.save(user);
        return UserDTO.fromEntity(user);
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        String token = jwtUtil.generateToken(user);

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(3600000L)
                .user(UserDTO.fromEntity(user))
                .build();
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return UserDTO.fromEntity(user);
    }

    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return UserDTO.fromEntity(user);
    }
}
