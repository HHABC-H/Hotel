package com.hotel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.Constant;
import com.hotel.common.PageResult;
import com.hotel.common.Result;
import com.hotel.entity.User;
import com.hotel.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<PageResult<List<User>>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {

        Page<User> page = userService.lambdaQuery()
                .like(StringUtils.hasText(username), User::getUsername, username)
                .eq(StringUtils.hasText(role), User::getRole, role)
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getId)
                .page(new Page<>(pageNum, pageSize));

        page.getRecords().forEach(this::sanitizePassword);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<User> get(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        sanitizePassword(user);
        return Result.success(user);
    }

    @PostMapping
    public Result<User> save(@RequestBody User user) {
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword()) || !StringUtils.hasText(user.getRole())
                || !StringUtils.hasText(user.getRealName()) || !StringUtils.hasText(user.getPhone())) {
            return Result.error(400, "用户名、密码、角色、姓名、手机号不能为空");
        }
        if (userService.getByUsername(user.getUsername()) != null) {
            return Result.error(400, "用户名已存在");
        }
        if (StringUtils.hasText(user.getIdCard())) {
            boolean idCardExists = userService.lambdaQuery().eq(User::getIdCard, user.getIdCard()).exists();
            if (idCardExists) {
                return Result.error(400, "身份证号已存在");
            }
        }

        user.setPassword(md5(user.getPassword()));
        if (!StringUtils.hasText(user.getGender())) {
            user.setGender("UNKNOWN");
        }
        if (user.getStatus() == null) {
            user.setStatus(Constant.STATUS_ENABLE);
        }
        user.setBalance(normalizeBalance(user.getBalance()));
        if (user.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            return Result.error(400, "余额不能为负数");
        }

        boolean saved = userService.save(user);
        if (!saved) {
            return Result.error("新增用户失败");
        }
        sanitizePassword(user);
        return Result.success(user);
    }

    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody User payload) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        if (StringUtils.hasText(payload.getUsername()) && !payload.getUsername().equals(user.getUsername())) {
            User existed = userService.getByUsername(payload.getUsername());
            if (existed != null && !existed.getId().equals(id)) {
                return Result.error(400, "用户名已存在");
            }
            user.setUsername(payload.getUsername());
        }
        if (StringUtils.hasText(payload.getRealName())) {
            user.setRealName(payload.getRealName());
        }
        if (StringUtils.hasText(payload.getPhone())) {
            user.setPhone(payload.getPhone());
        }
        if (StringUtils.hasText(payload.getRole())) {
            user.setRole(payload.getRole());
        }
        if (StringUtils.hasText(payload.getGender())) {
            user.setGender(payload.getGender());
        }
        if (StringUtils.hasText(payload.getIdCard())) {
            if (!payload.getIdCard().equals(user.getIdCard())) {
                boolean exists = userService.lambdaQuery().eq(User::getIdCard, payload.getIdCard()).ne(User::getId, id).exists();
                if (exists) {
                    return Result.error(400, "身份证号已存在");
                }
            }
            user.setIdCard(payload.getIdCard());
        }
        if (payload.getStatus() != null) {
            user.setStatus(payload.getStatus());
        }
        if (StringUtils.hasText(payload.getPassword())) {
            user.setPassword(md5(payload.getPassword()));
        }
        if (payload.getBalance() != null) {
            if (payload.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                return Result.error(400, "余额不能为负数");
            }
            user.setBalance(payload.getBalance());
        }

        userService.updateById(user);
        sanitizePassword(user);
        return Result.success(user);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return userService.removeById(id) ? Result.success() : Result.error("删除失败");
    }

    @PutMapping("/{id}/status")
    public Result<User> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        if (request.getStatus() == null) {
            return Result.error(400, "状态不能为空");
        }
        User user = userService.updateStatus(id, request.getStatus());
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        sanitizePassword(user);
        return Result.success(user);
    }

    private String md5(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }

    private BigDecimal normalizeBalance(BigDecimal balance) {
        return balance == null ? BigDecimal.ZERO : balance;
    }

    private void sanitizePassword(User user) {
        if (user != null) {
            user.setPassword(null);
        }
    }

    @Data
    public static class UpdateStatusRequest {
        private Integer status;
    }
}
