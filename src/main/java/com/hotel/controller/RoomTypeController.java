package com.hotel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.Constant;
import com.hotel.common.PageResult;
import com.hotel.common.Result;
import com.hotel.entity.RoomType;
import com.hotel.security.AuthUser;
import com.hotel.security.SecurityUtils;
import com.hotel.service.OssStorageService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;
    private final OssStorageService ossStorageService;

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
        if (!isCurrentAdmin() && StringUtils.hasText(roomType.getImg())) {
            return Result.error(403, "员工仅可查看房型图片，不能修改");
        }
        boolean saved = roomTypeService.save(roomType);
        return saved ? Result.success(roomType) : Result.error("新增失败");
    }

    @PutMapping("/{id}")
    public Result<RoomType> update(@PathVariable Long id, @RequestBody RoomType payload) {
        RoomType existing = roomTypeService.getById(id);
        if (existing == null) {
            return Result.error(404, "客房类型不存在");
        }

        if (!isCurrentAdmin()) {
            if (payload.getImg() != null && !Objects.equals(payload.getImg(), existing.getImg())) {
                return Result.error(403, "员工仅可查看房型图片，不能修改");
            }
            payload.setImg(existing.getImg());
        }

        payload.setId(id);
        boolean updated = roomTypeService.updateById(payload);
        return updated ? Result.success(payload) : Result.error("更新失败");
    }

    @PostMapping("/image/upload")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (!isCurrentAdmin()) {
            return Result.error(403, "只有管理员可以上传房型图片");
        }
        try {
            String url = ossStorageService.uploadRoomTypeImage(file);
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return Result.success(result);
        } catch (IllegalArgumentException ex) {
            return Result.error(400, ex.getMessage());
        } catch (IllegalStateException ex) {
            return Result.error(500, ex.getMessage());
        } catch (Exception ex) {
            return Result.error("上传房型图片失败");
        }
    }

    @DeleteMapping("/{id}/image")
    public Result<?> removeImage(@PathVariable Long id) {
        if (!isCurrentAdmin()) {
            return Result.error(403, "只有管理员可以删除房型图片");
        }
        RoomType roomType = roomTypeService.getById(id);
        if (roomType == null) {
            return Result.error(404, "客房类型不存在");
        }
        roomType.setImg(null);
        boolean updated = roomTypeService.updateById(roomType);
        return updated ? Result.success() : Result.error("删除房型图片失败");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return roomTypeService.removeById(id) ? Result.success() : Result.error("删除失败");
    }

    private boolean isCurrentAdmin() {
        AuthUser current = SecurityUtils.getCurrentUser();
        return current != null && Constant.ADMIN_ROLE.equals(current.getRole());
    }
}

