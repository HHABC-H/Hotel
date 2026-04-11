package com.hotel.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hotel.common.PageQuery;
import com.hotel.entity.User;
import com.hotel.mapper.UserMapper;
import com.hotel.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public IPage<User> getPageList(PageQuery pageQuery) {
        Page<User> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        return baseMapper.selectPage(page, null);
    }

    @Override
    public User getByUsername(String username) {
        return lambdaQuery().eq(User::getUsername, username).one();
    }

    @Override
    public User register(User user) {
        save(user);
        return user;
    }

    @Override
    public User updateStatus(Long id, Integer status) {
        User user = getById(id);
        if (user != null) {
            user.setStatus(status);
            updateById(user);
        }
        return user;
    }
}
