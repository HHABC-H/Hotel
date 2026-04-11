package com.hotel.controller;

import com.hotel.common.Result;
import com.hotel.entity.User;
import com.hotel.security.AuthUser;
import com.hotel.security.SecurityUtils;
import com.hotel.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public Result<?> getProfile() {
        AuthUser current = SecurityUtils.getCurrentUser();
        if (current == null) {
            return Result.error(401, "未认证");
        }

        User user = userService.getById(current.getUserId());
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        return Result.success(toProfile(user));
    }

    @PutMapping
    public Result<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        AuthUser current = SecurityUtils.getCurrentUser();
        if (current == null) {
            return Result.error(401, "未认证");
        }

        User user = userService.getById(current.getUserId());
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getIdCard())) {
            if (!request.getIdCard().equals(user.getIdCard())) {
                boolean exists = userService.lambdaQuery()
                        .eq(User::getIdCard, request.getIdCard())
                        .ne(User::getId, user.getId())
                        .exists();
                if (exists) {
                    return Result.error(400, "身份证号已存在");
                }
            }
            user.setIdCard(request.getIdCard());
        }
        if (StringUtils.hasText(request.getGender())) {
            user.setGender(request.getGender());
        }

        boolean updated = userService.updateById(user);
        return updated ? Result.success(toProfile(user)) : Result.error("更新个人信息失败");
    }

    private Map<String, Object> toProfile(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("realName", user.getRealName());
        profile.put("phone", user.getPhone());
        profile.put("idCard", user.getIdCard());
        profile.put("gender", user.getGender());
        profile.put("role", user.getRole());
        profile.put("status", user.getStatus());
        return profile;
    }

    @Data
    public static class UpdateProfileRequest {
        private String realName;
        private String phone;
        private String idCard;
        private String gender;
    }
}
