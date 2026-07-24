package com.example.codeeval.controller;

import com.example.codeeval.dto.ApiResponse;
import com.example.codeeval.dto.RegisterRequest;
import com.example.codeeval.dto.UserDTO;
import com.example.codeeval.entity.User;
import com.example.codeeval.repository.UserRepository;
import com.example.codeeval.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
public class AdminController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        List<UserDTO> users = userRepository.findAll().stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("查询成功", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("查询成功", user));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@RequestBody Map<String, Object> request) {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername((String) request.get("username"));
        registerRequest.setName((String) request.get("name"));
        registerRequest.setEmail((String) request.get("email"));
        registerRequest.setPassword((String) request.get("password"));
        registerRequest.setRole((String) request.get("role"));
        
        UserDTO user = userService.register(registerRequest);
        return ResponseEntity.ok(ApiResponse.success("创建成功", user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        if (request.containsKey("username")) {
            user.setUsername((String) request.get("username"));
        }
        if (request.containsKey("name")) {
            user.setName((String) request.get("name"));
        }
        if (request.containsKey("email")) {
            user.setEmail((String) request.get("email"));
        }
        if (request.containsKey("role")) {
            user.setRole(User.Role.valueOf(((String) request.get("role")).toUpperCase()));
        }
        if (request.containsKey("password") && request.get("password") != null && !((String) request.get("password")).isEmpty()) {
            user.setPassword(passwordEncoder.encode((String) request.get("password")));
        }
        
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("更新成功", UserDTO.fromEntity(user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("用户不存在");
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserDTO>> toggleStatus(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        if (user.getRole() == User.Role.ADMIN) {
            throw new IllegalArgumentException("不能禁用管理员账号");
        }
        
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("状态已更新", UserDTO.fromEntity(user)));
    }
}