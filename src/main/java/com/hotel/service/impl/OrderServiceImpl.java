package com.hotel.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hotel.common.PageQuery;
import com.hotel.entity.Order;
import com.hotel.mapper.OrderMapper;
import com.hotel.service.OrderService;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Override
    public IPage<Order> getPageList(PageQuery pageQuery) {
        Page<Order> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        return baseMapper.selectPage(page, null);
    }

    @Override
    public Order getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public Order saveOrder(Order order) {
        save(order);
        return order;
    }

    @Override
    public Order updateOrder(Order order) {
        updateById(order);
        return order;
    }

    @Override
    public boolean deleteById(Long id) {
        return removeById(id);
    }

    @Override
    public Order updateStatus(Long id, String status) {
        Order order = getById(id);
        if (order != null) {
            order.setStatus(status);
            updateById(order);
        }
        return order;
    }
}
