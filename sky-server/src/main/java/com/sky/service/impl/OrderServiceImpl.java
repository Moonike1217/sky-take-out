package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.util.BeanUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.DoubleStream.builder;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 新建订单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {

        //异常数据处理: 购物车为空、地址簿为空
        Long userId = BaseContext.getCurrentId();
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            //地址簿为空 抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        ShoppingCart shoppingCart = ShoppingCart.builder()
                                                .userId(userId)
                                                .build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            //购物车为空 抛出业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表中插入一条数据

        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setNumber(UUID.randomUUID().toString());
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getDetail());
        orders.setConsignee(addressBook.getConsignee());

        orderMapper.insert(orders);

        //订单明细表中插入若干条数据

        List<OrderDetail> orderDetailList = new ArrayList<>();

        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        //清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        //将返回数据封装到VO中
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .build();


        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

//        全部注释，跳过微信支付
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );

        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                //修改订单状态为待接单
                .status(Orders.TO_BE_CONFIRMED)
                //修改支付状态为已支付
                .payStatus(Orders.PAID)
                //修改结账时间为当前
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //通过WebSocketServer向客户端浏览器推送信息 含有三个字段:type orderId content 以JSON格式推送
        //先建立一个Map
        Map map = new HashMap<>();
        //数据类型 1为来单提醒 2为催单
        map.put("type", 1);
        //订单id
        map.put("orderId", ordersDB.getId());
        //提醒内容
        map.put("content", "订单号: " + outTradeNo);

        String json = JSONObject.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    @Transactional
    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        //初始化PageHelper
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        //设置需要查询记录的用户id，为将DTO传至Mapper层做准备
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());

        //调用Mapper进行分页查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        //查询后的结果应该通过VO返回，而不能直接通过entity(PO)返回
        List<OrderVO> orderVOList = new ArrayList<>();

        //遍历分页查询返回的page，通过Beanutils的属性拷贝，将page中的数据项拷贝至orderVOList中
        if (page != null && page.getTotal() > 0) {

            for (Orders order : page) {
                OrderVO item = new OrderVO();
                //对重复属性进行属性拷贝
                BeanUtils.copyProperties(order, item);
                //OrderVO是通过extends Orders得到的，所以需要额外补充两个属性
                //获取订单id
                Long orderId = order.getId();
                //获取订单详情
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);
                //为OrderVO设置OrderDetailList
                item.setOrderDetailList(orderDetailList);
                //封装完成，将当前item装入OrderVOList中
                orderVOList.add(item);
            }
        }

        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Transactional
    @Override
    public OrderVO detail(Long id) {
        //定义VO
        OrderVO orderVO = new OrderVO();
        //封装订单信息
        Orders orders = orderMapper.getById(id);
        BeanUtils.copyProperties(orders, orderVO);
        //封装订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        orderVO.setOrderDetailList(orderDetailList);
        //返回VO
        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    public void cancel(Long id) {
        //数据库中的该订单
        Orders ordersDB = orderMapper.getById(id);

        if (ordersDB == null) {
            //取消的订单不存在，抛出异常
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }


        Orders orders = new Orders();
        //设置id
        orders.setId(id);
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //待接单状态

            //执行退款操作...

            //修改订单状态
            orders.setPayStatus(Orders.REFUND);

        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
        return;
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        //将对应订单的商品重新加入到购物车中
//        //自己写法
//        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
//        for (OrderDetail orderDetail : orderDetailList) {
//            ShoppingCartDTO shoppingCartDTO = new ShoppingCartDTO();
//            BeanUtils.copyProperties(orderDetail, shoppingCartDTO);
//            shoppingCartService.add(shoppingCartDTO);
//        }

        //黑马写法
        List<ShoppingCart> shoppingCartList = orderDetailMapper.getByOrderId(id).stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            //拷贝相同属性(id字段不需要拷贝)
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());
        shoppingCartMapper.insertBatch(shoppingCartList);


        //构造一个DTO
        OrdersSubmitDTO ordersSubmitDTO = new OrdersSubmitDTO();
        Orders orders = orderMapper.getById(id);
        BeanUtils.copyProperties(orders, ordersSubmitDTO);
        if (orders.getDeliveryStatus() == 1) {
            ordersSubmitDTO.setEstimatedDeliveryTime(LocalDateTime.now().plusHours(1));
        }
        //调用submit函数重新下单
        submit(ordersSubmitDTO);
        return;
    }

    /**
     * 管理端订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //PageHelper初始化
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        //调用OrdersMapper查询相关订单
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        //分页查询应当通过VO返回结果
        List<OrderVO> orderVOList = new ArrayList<>();
        //遍历page
        if (page != null && !page.isEmpty()) {
            for (Orders orders : page) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                //填充orderDishes字段
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
                List<String> strings = orderDetailList.stream().map(x -> {
                    //菜品*份数 (如: 宫保鸡丁*3)
                    return x.getName() + "*" + x.getNumber();
                }).collect(Collectors.toList());
                orderVO.setOrderDishes(strings.toString());
                //将当前封装好的orderVO添加到List中
                orderVOList.add(orderVO);
            }
        }

        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 统计各状态订单数量
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        //订单状态  2待接单 3已接单 4派送中
        //待接单数量 private Integer toBeConfirmed;
        //待派送(已接单)数量 private Integer confirmed;
        //派送中数量 private Integer deliveryInProgress;
        Integer toBeConfirmed = orderMapper.countStatus(OrderVO.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(OrderVO.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(OrderVO.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        //修改订单状态为CONFIRMED = 3(已接单)
//        orderMapper.confirm(Math.toIntExact(ordersConfirmDTO.getId()));

        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        //只有订单存在并且订单状态为待接单时才能够拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //如果用户已支付则需要退款
        if (ordersDB.getPayStatus().equals(Orders.PAID)) {
            //执行退款逻辑...
            log.info("商家拒单，退款");
        }

        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .status(Orders.CANCELLED)
                .payStatus(Orders.REFUND)
                .cancelReason("商家拒单")
                .cancelTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 管理端取消订单
     * @param ordersCancelDTO
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        //只有订单存在才能够取消
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //如果用户已支付则需要退款
        if (ordersDB.getPayStatus().equals(Orders.PAID)) {
            //执行退款逻辑...
            log.info("商家取消订单，退款");
        }

        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .payStatus(Orders.REFUND)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        //只有 订单存在 并且 订单处在已接单状态 才能够派送订单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }
}

