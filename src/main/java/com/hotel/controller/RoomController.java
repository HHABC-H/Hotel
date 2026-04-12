package com.hotel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.Constant;
import com.hotel.common.PageResult;
import com.hotel.common.Result;
import com.hotel.entity.Order;
import com.hotel.entity.Room;
import com.hotel.entity.RoomType;
import com.hotel.service.OrderService;
import com.hotel.service.RoomService;
import com.hotel.service.RoomTypeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final OrderService orderService;
    private final RoomTypeService roomTypeService;

    @GetMapping
    public Result<PageResult<List<Room>>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roomNumber) {

        Page<Room> page = roomService.lambdaQuery()
                .eq(roomTypeId != null, Room::getRoomTypeId, roomTypeId)
                .eq(StringUtils.hasText(status), Room::getStatus, status)
                .like(StringUtils.hasText(roomNumber), Room::getRoomNumber, roomNumber)
                .orderByAsc(Room::getRoomNumber)
                .page(new Page<>(pageNum, pageSize));

        enrichRoomTypeInfo(page.getRecords());
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<Room> get(@PathVariable Long id) {
        Room room = roomService.getById(id);
        if (room == null) {
            return Result.error(404, "客房不存在");
        }
        enrichRoomTypeInfo(Arrays.asList(room));
        return Result.success(room);
    }

    @PostMapping
    public Result<Room> save(@RequestBody Room room) {
        if (!StringUtils.hasText(room.getRoomNumber()) || room.getRoomTypeId() == null || room.getFloor() == null) {
            return Result.error(400, "房间号、客房类型、楼层不能为空");
        }
        boolean exists = roomService.lambdaQuery().eq(Room::getRoomNumber, room.getRoomNumber()).exists();
        if (exists) {
            return Result.error(400, "房间号已存在");
        }
        if (!StringUtils.hasText(room.getStatus())) {
            room.setStatus(Constant.ROOM_STATUS_AVAILABLE);
        }
        return roomService.save(room) ? Result.success(room) : Result.error("新增失败");
    }

    @PutMapping("/{id}")
    public Result<Room> update(@PathVariable Long id, @RequestBody Room payload) {
        Room room = roomService.getById(id);
        if (room == null) {
            return Result.error(404, "客房不存在");
        }
        if (StringUtils.hasText(payload.getRoomNumber()) && !payload.getRoomNumber().equals(room.getRoomNumber())) {
            boolean exists = roomService.lambdaQuery().eq(Room::getRoomNumber, payload.getRoomNumber()).exists();
            if (exists) {
                return Result.error(400, "房间号已存在");
            }
        }
        payload.setId(id);
        return roomService.updateById(payload) ? Result.success(payload) : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return roomService.removeById(id) ? Result.success() : Result.error("删除失败");
    }

    @PutMapping("/{id}/status")
    public Result<Room> updateStatus(@PathVariable Long id, @RequestBody UpdateRoomStatusRequest request) {
        if (!StringUtils.hasText(request.getStatus())) {
            return Result.error(400, "状态不能为空");
        }
        Room room = roomService.updateStatus(id, request.getStatus());
        return room == null ? Result.error(404, "客房不存在") : Result.success(room);
    }

    @GetMapping("/available")
    public Result<List<Room>> availableRooms(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {

        if ((checkInDate == null) ^ (checkOutDate == null)) {
            return Result.error(400, "入住日期和退房日期必须同时提供");
        }

        return Result.success(getAvailableRooms(checkInDate, checkOutDate));
    }

    @GetMapping("/browse")
    public Result<List<Room>> browseRooms(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
            @RequestParam(required = false) Long roomTypeId) {

        if ((checkInDate == null) ^ (checkOutDate == null)) {
            return Result.error(400, "入住日期和退房日期必须同时提供");
        }

        List<Room> available = getAvailableRooms(checkInDate, checkOutDate);
        if (roomTypeId != null) {
            available = available.stream().filter(room -> roomTypeId.equals(room.getRoomTypeId())).collect(Collectors.toList());
        }
        return Result.success(available);
    }

    @GetMapping("/{id}/detail")
    public Result<Room> roomDetail(@PathVariable Long id) {
        return get(id);
    }

    private List<Room> getAvailableRooms(LocalDate checkInDate, LocalDate checkOutDate) {
        List<Room> baseRooms = roomService.lambdaQuery()
                .eq(Room::getStatus, Constant.ROOM_STATUS_AVAILABLE)
                .orderByAsc(Room::getRoomNumber)
                .list();

        if (checkInDate == null || checkOutDate == null) {
            enrichRoomTypeInfo(baseRooms);
            return baseRooms;
        }

        List<Order> overlapOrders = orderService.lambdaQuery()
                .in(Order::getStatus, Arrays.asList(Constant.ORDER_STATUS_UNPAID, Constant.ORDER_STATUS_PAID))
                .lt(Order::getCheckInDate, checkOutDate)
                .gt(Order::getCheckOutDate, checkInDate)
                .list();

        Set<Long> occupiedRoomIds = overlapOrders.stream().map(Order::getRoomId).collect(Collectors.toSet());
        List<Room> availableRooms = baseRooms.stream().filter(room -> !occupiedRoomIds.contains(room.getId())).collect(Collectors.toList());
        enrichRoomTypeInfo(availableRooms);
        return availableRooms;
    }

    private void enrichRoomTypeInfo(List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return;
        }
        Set<Long> roomTypeIds = rooms.stream()
                .map(Room::getRoomTypeId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (roomTypeIds.isEmpty()) {
            return;
        }

        Map<Long, RoomType> roomTypeMap = new HashMap<>();
        roomTypeService.listByIds(roomTypeIds).forEach(roomType -> roomTypeMap.put(roomType.getId(), roomType));

        rooms.forEach(room -> {
            RoomType roomType = roomTypeMap.get(room.getRoomTypeId());
            if (roomType != null) {
                room.setRoomTypeName(roomType.getTypeName());
                room.setReferencePrice(roomType.getPrice());
                room.setRoomTypeCapacity(roomType.getCapacity());
                room.setRoomTypeBedType(roomType.getBedType());
                room.setRoomTypeArea(roomType.getArea());
                room.setRoomTypeDescription(roomType.getDescription());
            }
        });
    }

    @Data
    public static class UpdateRoomStatusRequest {
        private String status;
    }
}
