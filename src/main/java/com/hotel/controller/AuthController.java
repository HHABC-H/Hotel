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

import java.math.BigDecimal;
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

    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getUsername())
                || !StringUtils.hasText(request.getPassword())
                || !StringUtils.hasText(request.getRealName())
                || !StringUtils.hasText(request.getPhone())) {
            return Result.error(400, "用户名、密码、姓名、手机号不能为空");
        }
        if (!request.getPhone().matches("^1\\d{10}$")) {
            return Result.error(400, "手机号格式不正确");
        }
        if (StringUtils.hasText(request.getIdCard()) && !request.getIdCard().matches("^\\d{17}(\\d|X|x)$")) {
            return Result.error(400, "身份证号格式不正确");
        }
        if (request.getPassword().length() < 6) {
            return Result.error(400, "密码长度至少 6 位");
        }
        if (userService.getByUsername(request.getUsername().trim()) != null) {
            return Result.error(400, "用户名已存在");
        }
        if (StringUtils.hasText(request.getIdCard())) {
            boolean exists = userService.lambdaQuery().eq(User::getIdCard, request.getIdCard()).exists();
            if (exists) {
                return Result.error(400, "身份证号已存在");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(DigestUtils.md5DigestAsHex(request.getPassword().getBytes(StandardCharsets.UTF_8)));
        user.setRealName(request.getRealName().trim());
        user.setPhone(request.getPhone().trim());
        user.setIdCard(StringUtils.hasText(request.getIdCard()) ? request.getIdCard().trim() : null);
        user.setGender(StringUtils.hasText(request.getGender()) ? request.getGender() : "UNKNOWN");
        user.setRole(Constant.CLIENT_ROLE);
        user.setStatus(Constant.STATUS_ENABLE);
        user.setBalance(BigDecimal.ZERO);

        boolean saved = userService.save(user);
        if (!saved) {
            return Result.error("注册失败");
        }
        return Result.success("注册成功", sanitizeUser(user));
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

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String realName;
        private String phone;
        private String idCard;
        private String gender;
    }
}
