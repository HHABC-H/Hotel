package com.hotel.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hotel.common.PageQuery;
import com.hotel.entity.Room;
import com.hotel.mapper.RoomMapper;
import com.hotel.service.RoomService;
import org.springframework.stereotype.Service;

@Service
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements RoomService {

    @Override
    public IPage<Room> getPageList(PageQuery pageQuery) {
        Page<Room> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        return baseMapper.selectPage(page, null);
    }

    @Override
    public Room getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public Room saveRoom(Room room) {
        save(room);
        return room;
    }

    @Override
    public Room updateRoom(Room room) {
        updateById(room);
        return room;
    }

    @Override
    public boolean deleteById(Long id) {
        return removeById(id);
    }

    @Override
    public Room updateStatus(Long id, String status) {
        Room room = getById(id);
        if (room != null) {
            room.setStatus(status);
            updateById(room);
        }
        return room;
    }
}
