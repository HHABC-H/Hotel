package com.hotel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hotel.common.PageQuery;
import com.hotel.entity.User;

public interface UserService extends IService<User> {

    IPage<User> getPageList(PageQuery pageQuery);

    User getByUsername(String username);

    User register(User user);

    User updateStatus(Long id, Integer status);
}
