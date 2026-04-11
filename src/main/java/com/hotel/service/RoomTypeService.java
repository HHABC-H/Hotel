package com.hotel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hotel.common.PageQuery;
import com.hotel.entity.RoomType;

public interface RoomTypeService extends IService<RoomType> {

    IPage<RoomType> getPageList(PageQuery pageQuery);

    RoomType getById(Long id);

    RoomType saveRoomType(RoomType roomType);

    RoomType updateRoomType(RoomType roomType);

    boolean deleteById(Long id);
}
