package com.hotel.controller;

import com.hotel.common.Constant;
import com.hotel.common.Result;
import com.hotel.entity.Order;
import com.hotel.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final OrderService orderService;

    @GetMapping("/stats")
    public Result<DashboardStatsResponse> stats() {
        List<Order> orders = orderService.list();

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime sevenDaysStart = today.minusDays(6).atStartOfDay();
        LocalDateTime thirtyDaysStart = today.minusDays(29).atStartOfDay();

        long totalOrderCount = orders.size();
        long todayOrderCount = 0L;
        long accumulatedCheckInCount = 0L;

        BigDecimal totalRevenueAmount = BigDecimal.ZERO;
        BigDecimal todayAmount = BigDecimal.ZERO;
        BigDecimal last7DaysAmount = BigDecimal.ZERO;
        BigDecimal last30DaysAmount = BigDecimal.ZERO;

        for (Order order : orders) {
            LocalDateTime createTime = order.getCreateTime();
            if (inRange(createTime, todayStart, tomorrowStart)) {
                todayOrderCount++;
            }

            if (!isRevenueOrder(order)) {
                continue;
            }

            BigDecimal amount = safeAmount(order);
            totalRevenueAmount = totalRevenueAmount.add(amount);

            if (inRange(createTime, todayStart, tomorrowStart)) {
                todayAmount = todayAmount.add(amount);
            }
            if (inRange(createTime, sevenDaysStart, tomorrowStart)) {
                last7DaysAmount = last7DaysAmount.add(amount);
            }
            if (inRange(createTime, thirtyDaysStart, tomorrowStart)) {
                last30DaysAmount = last30DaysAmount.add(amount);
            }

            if (order.getCheckInDate() != null && !order.getCheckInDate().isAfter(today)) {
                accumulatedCheckInCount++;
            }
        }

        DashboardStatsResponse response = new DashboardStatsResponse();
        response.setTotalRevenueAmount(scale(totalRevenueAmount));
        response.setAccumulatedCheckInCount(accumulatedCheckInCount);
        response.setTodayAmount(scale(todayAmount));
        response.setLast7DaysAmount(scale(last7DaysAmount));
        response.setLast30DaysAmount(scale(last30DaysAmount));
        response.setTodayOrderCount(todayOrderCount);
        response.setTotalOrderCount(totalOrderCount);
        return Result.success(response);
    }

    private boolean isRevenueOrder(Order order) {
        return Constant.ORDER_STATUS_PAID.equals(order.getStatus())
                || Constant.ORDER_STATUS_COMPLETED.equals(order.getStatus());
    }

    private boolean inRange(LocalDateTime value, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return value != null && !value.isBefore(startInclusive) && value.isBefore(endExclusive);
    }

    private BigDecimal safeAmount(Order order) {
        return order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    @Data
    public static class DashboardStatsResponse {
        private BigDecimal totalRevenueAmount;
        private Long accumulatedCheckInCount;
        private BigDecimal todayAmount;
        private BigDecimal last7DaysAmount;
        private BigDecimal last30DaysAmount;
        private Long todayOrderCount;
        private Long totalOrderCount;
    }
}

