package com.hotel.controller;

import com.hotel.common.Constant;
import com.hotel.common.Result;
import com.hotel.entity.Order;
import com.hotel.entity.Room;
import com.hotel.entity.RoomType;
import com.hotel.entity.User;
import com.hotel.security.AuthUser;
import com.hotel.security.SecurityUtils;
import com.hotel.service.OrderService;
import com.hotel.service.RoomService;
import com.hotel.service.RoomTypeService;
import com.hotel.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final OrderService orderService;
    private final RoomService roomService;
    private final RoomTypeService roomTypeService;
    private final UserService userService;

    @PostMapping
    @Transactional
    public Result<Order> booking(@RequestBody BookingRequest request) {
        AuthUser current = SecurityUtils.getCurrentUser();
        if (current == null) {
            return Result.error(401, "未认证");
        }
        if (!Constant.CLIENT_ROLE.equals(current.getRole())) {
            return Result.error(403, "仅客户角色可使用在线预订");
        }
        User currentUser = userService.getById(current.getUserId());
        if (currentUser == null) {
            return Result.error(404, "当前客户不存在");
        }
        if (!Constant.CLIENT_ROLE.equals(currentUser.getRole())) {
            return Result.error(403, "当前用户不是客户角色");
        }
        if (!Constant.STATUS_ENABLE.equals(currentUser.getStatus())) {
            return Result.error(400, "账号已被禁用，无法预订");
        }

        if (request == null || request.getRoomId() == null
                || request.getCheckInDate() == null || request.getCheckOutDate() == null) {
            return Result.error(400, "客房、入住日期、退房日期不能为空");
        }
        if (!request.getCheckInDate().isBefore(request.getCheckOutDate())) {
            return Result.error(400, "退房日期必须晚于入住日期");
        }

        Room room = roomService.getById(request.getRoomId());
        if (room == null) {
            return Result.error(404, "客房不存在");
        }

        RoomType roomType = roomTypeService.getById(room.getRoomTypeId());
        if (roomType == null || roomType.getPrice() == null) {
            return Result.error(400, "客房类型价格未配置");
        }

        boolean overlap = orderService.lambdaQuery()
                .eq(Order::getRoomId, request.getRoomId())
                .in(Order::getStatus, Arrays.asList(Constant.ORDER_STATUS_UNPAID, Constant.ORDER_STATUS_PAID))
                .lt(Order::getCheckInDate, request.getCheckOutDate())
                .gt(Order::getCheckOutDate, request.getCheckInDate())
                .exists();
        if (overlap) {
            return Result.error(400, "该时间段客房已被预订");
        }

        long days = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalAmount = roomType.getPrice().multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
        if (safeBalance(currentUser).compareTo(totalAmount) < 0) {
            return Result.error(400, "余额不足，请先充值");
        }
        if (!deductBalance(currentUser.getId(), totalAmount)) {
            return Result.error(400, "余额不足，请先充值");
        }

        Order order = new Order();
        order.setOrderNumber("BK" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
        order.setCustomerId(currentUser.getId());
        order.setRoomId(request.getRoomId());
        order.setCheckInDate(request.getCheckInDate());
        order.setCheckOutDate(request.getCheckOutDate());
        order.setTotalAmount(totalAmount);
        order.setStatus(Constant.ORDER_STATUS_PAID);
        order.setRemark(request.getRemark());
        order.setCreateUserId(currentUser.getId());

        try {
            boolean saved = orderService.save(order);
            if (!saved) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("预订失败");
            }
            return Result.success(order);
        } catch (DataIntegrityViolationException ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error(400, "预订失败：客户或客房数据异常，请刷新后重试");
        }
    }

    @GetMapping("/my")
    public Result<List<Order>> myBookings() {
        AuthUser current = SecurityUtils.getCurrentUser();
        if (current == null) {
            return Result.error(401, "未认证");
        }
        if (!Constant.CLIENT_ROLE.equals(current.getRole())) {
            return Result.error(403, "仅客户角色可查看我的预订");
        }
        List<Order> bookings = orderService.lambdaQuery()
                .eq(Order::getCustomerId, current.getUserId())
                .orderByDesc(Order::getId)
                .list();
        return Result.success(bookings);
    }

    @Data
    public static class BookingRequest {
        private Long roomId;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate checkInDate;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate checkOutDate;
        private String remark;
    }

    private BigDecimal safeBalance(User user) {
        return user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
    }

    private boolean deductBalance(Long userId, BigDecimal amount) {
        return userService.lambdaUpdate()
                .eq(User::getId, userId)
                .ge(User::getBalance, amount)
                .setSql("balance = balance - " + amount.toPlainString())
                .update();
    }
}
