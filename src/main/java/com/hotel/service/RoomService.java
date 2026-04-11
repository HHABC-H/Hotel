package com.hotel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hotel.common.PageQuery;
import com.hotel.entity.Room;

public interface RoomService extends IService<Room> {

    IPage<Room> getPageList(PageQuery pageQuery);

    Room getById(Long id);

    Room saveRoom(Room room);

    Room updateRoom(Room room);

    boolean deleteById(Long id);

    Room updateStatus(Long id, String status);
}
