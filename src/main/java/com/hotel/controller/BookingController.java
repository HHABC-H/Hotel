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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
        User currentUser = requireCurrentClient();
        if (currentUser == null) {
            return Result.error(401, "Unauthorized");
        }

        if (request == null || request.getRoomId() == null
                || request.getCheckInDate() == null || request.getCheckOutDate() == null) {
            return Result.error(400, "Room and dates are required");
        }
        if (!request.getCheckInDate().isBefore(request.getCheckOutDate())) {
            return Result.error(400, "Check-out date must be later than check-in date");
        }

        Room room = roomService.getById(request.getRoomId());
        if (room == null) {
            return Result.error(404, "Room not found");
        }

        RoomType roomType = roomTypeService.getById(room.getRoomTypeId());
        if (roomType == null || roomType.getPrice() == null) {
            return Result.error(400, "Room type price is not configured");
        }

        boolean overlap = orderService.lambdaQuery()
                .eq(Order::getRoomId, request.getRoomId())
                .in(Order::getStatus, Arrays.asList(Constant.ORDER_STATUS_UNPAID, Constant.ORDER_STATUS_PAID))
                .lt(Order::getCheckInDate, request.getCheckOutDate())
                .gt(Order::getCheckOutDate, request.getCheckInDate())
                .exists();
        if (overlap) {
            return Result.error(400, "Room already booked for this date range");
        }

        long days = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalAmount = roomType.getPrice().multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setOrderNumber("BK" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
        order.setCustomerId(currentUser.getId());
        order.setRoomId(request.getRoomId());
        order.setCheckInDate(request.getCheckInDate());
        order.setCheckOutDate(request.getCheckOutDate());
        order.setTotalAmount(totalAmount);
        order.setStatus(Constant.ORDER_STATUS_UNPAID);
        order.setRemark(request.getRemark());
        order.setCreateUserId(currentUser.getId());

        try {
            boolean saved = orderService.save(order);
            if (!saved) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("Create booking failed");
            }
            return Result.success(order);
        } catch (DataIntegrityViolationException ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error(400, "Create booking failed");
        }
    }

    @PutMapping("/{id}/pay")
    @Transactional
    public Result<Order> payBooking(@PathVariable Long id) {
        User currentUser = requireCurrentClient();
        if (currentUser == null) {
            return Result.error(401, "Unauthorized");
        }

        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error(404, "Order not found");
        }
        if (!currentUser.getId().equals(order.getCustomerId())) {
            return Result.error(403, "No permission");
        }
        if (!Constant.ORDER_STATUS_UNPAID.equals(order.getStatus())) {
            return Result.error(400, "Only unpaid orders can be paid");
        }
        if (safeBalance(currentUser).compareTo(order.getTotalAmount()) < 0) {
            return Result.error(400, "Insufficient balance");
        }
        if (!deductBalance(currentUser.getId(), order.getTotalAmount())) {
            return Result.error(400, "Insufficient balance");
        }

        order.setStatus(Constant.ORDER_STATUS_PAID);
        boolean updated = orderService.updateById(order);
        if (!updated) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("Pay booking failed");
        }
        return Result.success(order);
    }

    @DeleteMapping("/{id}/cancel")
    @Transactional
    public Result<?> cancelBooking(@PathVariable Long id) {
        User currentUser = requireCurrentClient();
        if (currentUser == null) {
            return Result.error(401, "Unauthorized");
        }

        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error(404, "Order not found");
        }
        if (!currentUser.getId().equals(order.getCustomerId())) {
            return Result.error(403, "No permission");
        }
        if (Constant.ORDER_STATUS_CANCELLED.equals(order.getStatus())) {
            return Result.success("Order already cancelled", null);
        }
        if (Constant.ORDER_STATUS_COMPLETED.equals(order.getStatus())) {
            return Result.error(400, "Completed order cannot be cancelled");
        }
        if (!Constant.ORDER_STATUS_UNPAID.equals(order.getStatus())
                && !Constant.ORDER_STATUS_PAID.equals(order.getStatus())) {
            return Result.error(400, "Only unpaid or paid orders can be cancelled online");
        }
        LocalDate today = LocalDate.now();
        if (today.isAfter(order.getCheckOutDate())) {
            return Result.error(400, "Booking time has passed, cancellation is not allowed");
        }

        Room room = roomService.getById(order.getRoomId());
        if (room != null && Constant.ROOM_STATUS_OCCUPIED.equals(room.getStatus())) {
            return Result.error(400, "Occupied order cannot be cancelled");
        }

        if (Constant.ORDER_STATUS_PAID.equals(order.getStatus())
                && !refundBalance(currentUser.getId(), order.getTotalAmount())) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("Refund failed when cancel booking");
        }

        order.setStatus(Constant.ORDER_STATUS_CANCELLED);
        return orderService.updateById(order) ? Result.success() : Result.error("Cancel booking failed");
    }

    @GetMapping("/my")
    public Result<List<Order>> myBookings() {
        User currentUser = requireCurrentClient();
        if (currentUser == null) {
            return Result.error(401, "Unauthorized");
        }
        List<Order> bookings = orderService.lambdaQuery()
                .eq(Order::getCustomerId, currentUser.getId())
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

    private User requireCurrentClient() {
        AuthUser current = SecurityUtils.getCurrentUser();
        if (current == null || !Constant.CLIENT_ROLE.equals(current.getRole())) {
            return null;
        }
        User user = userService.getById(current.getUserId());
        if (user == null || !Constant.CLIENT_ROLE.equals(user.getRole())) {
            return null;
        }
        if (!Constant.STATUS_ENABLE.equals(user.getStatus())) {
            return null;
        }
        return user;
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

    private boolean refundBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        return userService.lambdaUpdate()
                .eq(User::getId, userId)
                .setSql("balance = IFNULL(balance, 0) + " + amount.toPlainString())
                .update();
    }
}
