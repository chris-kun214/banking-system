package com.banking.service;

import com.banking.dto.AuthResponse;
import com.banking.dto.LoginRequest;
import com.banking.dto.RegisterRequest;
import com.banking.dto.UserDTO;
import com.banking.entity.User;
import com.banking.repository.UserRepository;
import com.banking.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    /**
     * 用户登录
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) {
        logger.info("用户登录请求: {}", loginRequest.getUsername());

        // 认证用户
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 生成 JWT Token
        String jwt = jwtUtils.generateToken(loginRequest.getUsername());

        // 获取用户信息
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        logger.info("用户登录成功: {}", loginRequest.getUsername());

        return new AuthResponse(
                jwt,
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    /**
     * 用户注册
     */
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        logger.info("用户注册请求: {}", registerRequest.getUsername());

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("用户名已存在: " + registerRequest.getUsername());
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("邮箱已被注册: " + registerRequest.getEmail());
        }

        // 创建新用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setFullName(registerRequest.getFullName());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setRole(User.Role.USER);
        user.setEnabled(true);

        userRepository.save(user);

        // 生成 JWT Token
        String jwt = jwtUtils.generateToken(user.getUsername());

        logger.info("用户注册成功: {}", user.getUsername());

        return new AuthResponse(
                jwt,
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    /**
     * 获取当前登录用户信息
     */
    @Transactional(readOnly = true)
    public UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return UserDTO.fromEntity(user);
    }
}

