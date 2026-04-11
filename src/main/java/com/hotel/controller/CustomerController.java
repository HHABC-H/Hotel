package com.hotel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final UserService userService;

    @GetMapping
    public Result<PageResult<List<User>>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, Constant.CLIENT_ROLE)
                .orderByDesc(User::getId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getRealName, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getIdCard, keyword)
                    .or().like(User::getUsername, keyword));
        }

        Page<User> page = userService.page(new Page<>(pageNum, pageSize), wrapper);
        page.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<User> get(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null || !Constant.CLIENT_ROLE.equals(user.getRole())) {
            return Result.error(404, "客户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping
    public Result<User> save(@RequestBody SaveCustomerRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())
                || !StringUtils.hasText(request.getRealName()) || !StringUtils.hasText(request.getPhone())) {
            return Result.error(400, "用户名、密码、姓名、手机号不能为空");
        }
        if (userService.getByUsername(request.getUsername()) != null) {
            return Result.error(400, "用户名已存在");
        }
        if (StringUtils.hasText(request.getIdCard())) {
            boolean exists = userService.lambdaQuery().eq(User::getIdCard, request.getIdCard()).exists();
            if (exists) {
                return Result.error(400, "身份证号已存在");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(md5(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setIdCard(request.getIdCard());
        user.setGender(StringUtils.hasText(request.getGender()) ? request.getGender() : "UNKNOWN");
        user.setRole(Constant.CLIENT_ROLE);
        user.setStatus(request.getStatus() == null ? Constant.STATUS_ENABLE : request.getStatus());

        boolean saved = userService.save(user);
        user.setPassword(null);
        return saved ? Result.success(user) : Result.error("新增失败");
    }

    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody UpdateCustomerRequest request) {
        User user = userService.getById(id);
        if (user == null || !Constant.CLIENT_ROLE.equals(user.getRole())) {
            return Result.error(404, "客户不存在");
        }

        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getGender())) {
            user.setGender(request.getGender());
        }
        if (StringUtils.hasText(request.getIdCard())) {
            if (!request.getIdCard().equals(user.getIdCard())) {
                boolean exists = userService.lambdaQuery().eq(User::getIdCard, request.getIdCard()).ne(User::getId, id).exists();
                if (exists) {
                    return Result.error(400, "身份证号已存在");
                }
            }
            user.setIdCard(request.getIdCard());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(md5(request.getPassword()));
        }

        boolean updated = userService.updateById(user);
        user.setPassword(null);
        return updated ? Result.success(user) : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null || !Constant.CLIENT_ROLE.equals(user.getRole())) {
            return Result.error(404, "客户不存在");
        }
        return userService.removeById(id) ? Result.success() : Result.error("删除失败");
    }

    private String md5(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }

    @Data
    public static class SaveCustomerRequest {
        private String username;
        private String password;
        private String realName;
        private String phone;
        private String idCard;
        private String gender;
        private Integer status;
    }

    @Data
    public static class UpdateCustomerRequest {
        private String password;
        private String realName;
        private String phone;
        private String idCard;
        private String gender;
        private Integer status;
    }
}