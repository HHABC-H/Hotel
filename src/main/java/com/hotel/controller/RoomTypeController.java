package com.hotel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.PageResult;
import com.hotel.common.Result;
import com.hotel.entity.RoomType;
import com.hotel.service.RoomTypeService;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequestMapping("/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @GetMapping
    public Result<PageResult<List<RoomType>>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String typeName,
            @RequestParam(required = false) Integer status) {
        Page<RoomType> page = roomTypeService.lambdaQuery()
                .like(StringUtils.hasText(typeName), RoomType::getTypeName, typeName)
                .eq(status != null, RoomType::getStatus, status)
                .orderByDesc(RoomType::getId)
                .page(new Page<>(pageNum, pageSize));

        return Result.success(new PageResult<>(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<RoomType> get(@PathVariable Long id) {
        RoomType roomType = roomTypeService.getById(id);
        if (roomType == null) {
            return Result.error(404, "客房类型不存在");
        }
        return Result.success(roomType);
    }

    @PostMapping
    public Result<RoomType> save(@RequestBody RoomType roomType) {
        if (!StringUtils.hasText(roomType.getTypeName()) || roomType.getPrice() == null || roomType.getCapacity() == null) {
            return Result.error(400, "类型名称、价格、可住人数不能为空");
        }
        boolean saved = roomTypeService.save(roomType);
        return saved ? Result.success(roomType) : Result.error("新增失败");
    }

    @PutMapping("/{id}")
    public Result<RoomType> update(@PathVariable Long id, @RequestBody RoomType payload) {
        RoomType roomType = roomTypeService.getById(id);
        if (roomType == null) {
            return Result.error(404, "客房类型不存在");
        }
        payload.setId(id);
        boolean updated = roomTypeService.updateById(payload);
        return updated ? Result.success(payload) : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return roomTypeService.removeById(id) ? Result.success() : Result.error("删除失败");
    }
}