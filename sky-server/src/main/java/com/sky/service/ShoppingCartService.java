package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.ShoppingCartMapper;

import java.util.List;

public interface ShoppingCartService {
    /**
     * 新增购物车
     * @param shoppingCartDTO
     */
    void add (ShoppingCartDTO shoppingCartDTO);

    /**
     * 查询购物车
     */
    List<ShoppingCart> list();

    /**
     * 清空购物车
     */
    void delete();

    /**
     * 减少购物车中商品的数量
     * @param shoppingCartDTO
     */
    void sub(ShoppingCartDTO shoppingCartDTO);
}
