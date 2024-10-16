package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 插入新数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 查询历史订单（分页查询）
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from sky_take_out.orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 删除订单
     * @param id
     */
    @Delete("delete from sky_take_out.orders where id = #{id}")
    void deleteById(Long id);

    /**
     * 统计指定状态的订单数量
     * @param status
     * @return
     */
    @Select("select count(sky_take_out.orders.id) from sky_take_out.orders where status = #{status};")
    Integer countStatus(Integer status);

    /**
     * 接单
     * @param id
     */
    @Update("update sky_take_out.orders set status = 3 where id = #{id}")
    void confirm(Integer id);

    /**
     * 查询超时未支付订单
     * @param pendingPayment
     * @param outTime
     * @return
     */
    @Select("select * from sky_take_out.orders where status = #{pendingPayment} and order_time < #{outTime};")
    List<Orders> getByStatusAndOrderTimeLT(Integer pendingPayment, LocalDateTime outTime);
}
