package com.hotel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.Constant;
import com.hotel.common.PageResult;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final RoomService roomService;
    private final RoomTypeService roomTypeService;

    @GetMapping
    public Result<PageResult<List<Order>>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInEnd) {

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .like(StringUtils.hasText(orderNumber), Order::getOrderNumber, orderNumber)
                .eq(customerId != null, Order::getCustomerId, customerId)
                .eq(roomId != null, Order::getRoomId, roomId)
                .eq(StringUtils.hasText(status), Order::getStatus, status)
                .ge(checkInStart != null, Order::getCheckInDate, checkInStart)
                .le(checkInEnd != null, Order::getCheckInDate, checkInEnd)
                .orderByDesc(Order::getId);

        Page<Order> page = orderService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<Order> get(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        return Result.success(order);
    }

    @PostMapping
    @Transactional
    public Result<Order> save(@RequestBody CreateOrderRequest request) {
        AuthUser current = SecurityUtils.getCurrentUser();
        if (current == null) {
            return Result.error(401, "未认证");
        }

        Result<Order> validated = validateAndBuildOrder(request, current.getUserId(), null);
        if (validated.getCode() != 200) {
            return validated;
        }

        Order order = validated.getData();
        boolean saved = orderService.save(order);
        return saved ? Result.success(order) : Result.error("创建订单失败");
    }

    @PutMapping("/{id}")
    @Transactional
    public Result<Order> update(@PathVariable Long id, @RequestBody CreateOrderRequest request) {
        Order existing = orderService.getById(id);
        if (existing == null) {
            return Result.error(404, "订单不存在");
        }
        if (Constant.ORDER_STATUS_COMPLETED.equals(existing.getStatus()) || Constant.ORDER_STATUS_CANCELLED.equals(existing.getStatus())) {
            return Result.error(400, "已完成或已取消订单不可修改");
        }

        Result<Order> validated = validateAndBuildOrder(request, existing.getCreateUserId(), id);
        if (validated.getCode() != 200) {
            return validated;
        }

        Order update = validated.getData();
        update.setId(id);
        update.setOrderNumber(existing.getOrderNumber());
        update.setStatus(existing.getStatus());

        boolean updated = orderService.updateById(update);
        return updated ? Result.success(update) : Result.error("更新订单失败");
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<?> delete(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (Constant.ORDER_STATUS_CANCELLED.equals(order.getStatus())) {
            return Result.success("订单已取消", null);
        }
        if (Constant.ORDER_STATUS_COMPLETED.equals(order.getStatus())) {
            return Result.error(400, "已完成订单不可取消");
        }

        Room room = roomService.getById(order.getRoomId());
        if (room != null && Constant.ROOM_STATUS_OCCUPIED.equals(room.getStatus())) {
            return Result.error(400, "当前订单已入住，不可取消");
        }

        order.setStatus(Constant.ORDER_STATUS_CANCELLED);
        return orderService.updateById(order) ? Result.success() : Result.error("取消订单失败");
    }

    @PutMapping("/{id}/pay")
    @Transactional
    public Result<Order> pay(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (!Constant.ORDER_STATUS_UNPAID.equals(order.getStatus())) {
            return Result.error(400, "只有未支付订单可支付");
        }

        User customer = userService.getById(order.getCustomerId());
        if (customer == null || !Constant.CLIENT_ROLE.equals(customer.getRole())) {
            return Result.error(404, "客户不存在");
        }
        if (safeBalance(customer).compareTo(order.getTotalAmount()) < 0) {
            return Result.error(400, "余额不足，请先充值");
        }
        if (!deductBalance(customer.getId(), order.getTotalAmount())) {
            return Result.error(400, "余额不足，请先充值");
        }

        order.setStatus(Constant.ORDER_STATUS_PAID);
        boolean updated = orderService.updateById(order);
        if (!updated) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("订单支付失败");
        }
        return Result.success(order);
    }

    @PutMapping("/{id}/check-in")
    @Transactional
    public Result<Order> checkIn(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (!Constant.ORDER_STATUS_PAID.equals(order.getStatus())) {
            return Result.error(400, "只有已支付订单可办理入住");
        }

        Room room = roomService.getById(order.getRoomId());
        if (room == null) {
            return Result.error(404, "客房不存在");
        }
        if (!Constant.ROOM_STATUS_AVAILABLE.equals(room.getStatus())) {
            return Result.error(400, "客房不是可入住状态");
        }

        room.setStatus(Constant.ROOM_STATUS_OCCUPIED);
        roomService.updateById(room);
        return Result.success(order);
    }

    @PutMapping("/{id}/check-out")
    @Transactional
    public Result<Order> checkOut(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (!Constant.ORDER_STATUS_PAID.equals(order.getStatus())) {
            return Result.error(400, "只有已支付订单可办理退房");
        }

        Room room = roomService.getById(order.getRoomId());
        if (room == null) {
            return Result.error(404, "客房不存在");
        }

        order.setStatus(Constant.ORDER_STATUS_COMPLETED);
        room.setStatus(Constant.ROOM_STATUS_AVAILABLE);
        orderService.updateById(order);
        roomService.updateById(room);
        return Result.success(order);
    }

    @GetMapping("/my")
    public Result<List<Order>> myOrders() {
        AuthUser current = SecurityUtils.getCurrentUser();
        if (current == null) {
            return Result.error(401, "未认证");
        }

        List<Order> orders;
        if (Constant.CLIENT_ROLE.equals(current.getRole())) {
            orders = orderService.lambdaQuery()
                    .eq(Order::getCustomerId, current.getUserId())
                    .orderByDesc(Order::getId)
                    .list();
        } else {
            orders = orderService.lambdaQuery()
                    .eq(Order::getCreateUserId, current.getUserId())
                    .orderByDesc(Order::getId)
                    .list();
        }
        return Result.success(orders);
    }

    private Result<Order> validateAndBuildOrder(CreateOrderRequest request, Long createUserId, Long currentOrderId) {
        if (request == null || request.getCustomerId() == null || request.getRoomId() == null
                || request.getCheckInDate() == null || request.getCheckOutDate() == null) {
            return Result.error(400, "客户、客房、入住日期、退房日期不能为空");
        }
        if (!request.getCheckInDate().isBefore(request.getCheckOutDate())) {
            return Result.error(400, "退房日期必须晚于入住日期");
        }

        User customer = userService.getById(request.getCustomerId());
        if (customer == null || !Constant.CLIENT_ROLE.equals(customer.getRole())) {
            return Result.error(404, "客户不存在");
        }
        if (!Constant.STATUS_ENABLE.equals(customer.getStatus())) {
            return Result.error(400, "客户账号已禁用");
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
                .ne(currentOrderId != null, Order::getId, currentOrderId)
                .lt(Order::getCheckInDate, request.getCheckOutDate())
                .gt(Order::getCheckOutDate, request.getCheckInDate())
                .exists();

        if (overlap) {
            return Result.error(400, "该时间段客房已被预订");
        }

        long days = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalAmount = roomType.getPrice().multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setRoomId(request.getRoomId());
        order.setCheckInDate(request.getCheckInDate());
        order.setCheckOutDate(request.getCheckOutDate());
        order.setRemark(request.getRemark());
        order.setCreateUserId(createUserId);
        order.setTotalAmount(totalAmount);

        if (currentOrderId == null) {
            order.setStatus(Constant.ORDER_STATUS_UNPAID);
            order.setOrderNumber(generateOrderNumber());
        }

        return Result.success(order);
    }

    private String generateOrderNumber() {
        return "OD" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
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

    @Data
    public static class CreateOrderRequest {
        private Long customerId;
        private Long roomId;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate checkInDate;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate checkOutDate;
        private String remark;
    }
}
