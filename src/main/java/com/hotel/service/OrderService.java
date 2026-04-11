package com.hotel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hotel.common.PageQuery;
import com.hotel.entity.Order;

public interface OrderService extends IService<Order> {

    IPage<Order> getPageList(PageQuery pageQuery);

    Order getById(Long id);

    Order saveOrder(Order order);

    Order updateOrder(Order order);

    boolean deleteById(Long id);

    Order updateStatus(Long id, String status);
}
