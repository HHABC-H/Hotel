package com.hotel.controller;

import com.hotel.common.Constant;
import com.hotel.common.Result;
import com.hotel.entity.User;
import com.hotel.security.AuthUser;
import com.hotel.security.SecurityUtils;
import com.hotel.service.UserService;
import com.hotel.util.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.token-expiration:1800}")
    private long tokenExpirationSeconds;

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            return Result.error(400, "用户名和密码不能为空");
        }

        User user = userService.getByUsername(request.getUsername().trim());
        if (user == null || !passwordMatched(request.getPassword(), user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }
        if (!Constant.STATUS_ENABLE.equals(user.getStatus())) {
            return Result.error(403, "账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getRealName(), user.getStatus());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", "Bearer");
        data.put("expiresIn", tokenExpirationSeconds);
        data.put("user", sanitizeUser(user));
        return Result.success("登录成功", data);
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        return Result.success("登出成功", null);
    }

    @GetMapping("/current-user")
    public Result<?> currentUser() {
        AuthUser current = SecurityUtils.getCurrentUser();
        if (current == null) {
            return Result.error(401, "未认证");
        }

        User user = userService.getById(current.getUserId());
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(sanitizeUser(user));
    }

    private boolean passwordMatched(String raw, String stored) {
        if (!StringUtils.hasText(stored)) {
            return false;
        }
        if (isBcryptHash(stored)) {
            return passwordEncoder.matches(raw, stored);
        }
        if (raw.equals(stored)) {
            return true;
        }
        String md5 = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
        return md5.equalsIgnoreCase(stored);
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    private Map<String, Object> sanitizeUser(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("realName", user.getRealName());
        profile.put("phone", user.getPhone());
        profile.put("idCard", user.getIdCard());
        profile.put("gender", user.getGender());
        profile.put("role", user.getRole());
        profile.put("status", user.getStatus());
        profile.put("balance", user.getBalance());
        return profile;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
