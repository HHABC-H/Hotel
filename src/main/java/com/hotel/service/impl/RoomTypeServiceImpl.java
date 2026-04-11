package com.hotel.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hotel.common.PageQuery;
import com.hotel.entity.RoomType;
import com.hotel.mapper.RoomTypeMapper;
import com.hotel.service.RoomTypeService;
import org.springframework.stereotype.Service;

@Service
public class RoomTypeServiceImpl extends ServiceImpl<RoomTypeMapper, RoomType> implements RoomTypeService {

    @Override
    public IPage<RoomType> getPageList(PageQuery pageQuery) {
        Page<RoomType> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        return baseMapper.selectPage(page, null);
    }

    @Override
    public RoomType getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public RoomType saveRoomType(RoomType roomType) {
        save(roomType);
        return roomType;
    }

    @Override
    public RoomType updateRoomType(RoomType roomType) {
        updateById(roomType);
        return roomType;
    }

    @Override
    public boolean deleteById(Long id) {
        return removeById(id);
    }
}
