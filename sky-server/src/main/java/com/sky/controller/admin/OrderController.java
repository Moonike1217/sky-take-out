package com.sky.controller.admin;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "订单相关接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @ApiOperation("订单搜索")
    @GetMapping("/conditionSearch")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("管理端订单搜索:{}", ordersPageQueryDTO);
        PageResult page = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(page);
    }

    /**
     * 统计各状态订单数量
     * @return
     */
    @ApiOperation("统计各状态订单数量")
    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> statistics() {
        log.info("统计各状态订单数量");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 管理端查询订单详情
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("管理端查询订单详情")
    public Result<OrderVO> details(@PathVariable Long id) {
        log.info("查询订单详情:{}", id);
        OrderVO orderVO = orderService.detail(id);
        return Result.success(orderVO);
    }

    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        log.info("接单:{}", ordersConfirmDTO);
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    @ApiOperation("拒单")
    @PutMapping("/rejection")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) {
        log.info("拒单:{}", ordersRejectionDTO);
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) {
        log.info("商家取消订单:{}", ordersCancelDTO);
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result delivery(@PathVariable Long id) {
        log.info("派送订单:{}", id);
        orderService.delivery(id);
        return Result.success();
    }

    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable Long id) {
        log.info("完成订单:{}", id);
        orderService.complete(id);
        return Result.success();
    }

}
