package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    /**
     * 动态查询
     * @param shoppingCart
     * @return
     */
    public List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 更新number字段
     * @param item
     */
    @Update("update sky_take_out.shopping_cart set number = #{number} where id = #{id}")
    void updateNumber(ShoppingCart item);

    /**
     * 插入新记录
     * @param shoppingCart
     */
    @Insert("insert into sky_take_out.shopping_cart " +
            "(name, image, user_id, dish_id, setmeal_id, dish_flavor, amount, create_time) " +
            "VALUES " +
            "(#{name}, #{image}, #{userId}, #{dishId}, #{setmealId}, #{dishFlavor}, #{amount}, #{createTime})")
    void insert(ShoppingCart shoppingCart);

    /**
     * 根据用户id清空购物车
     * @param currentId
     */
    @Delete("delete from sky_take_out.shopping_cart where user_id = #{currentId}")
    void deleteByUserId(Long currentId);

    /**
     * 根据主键id删除购物车中的某一项
     * @param cart
     */
    @Delete("delete from sky_take_out.shopping_cart where id = #{id}")
    void deleteById(ShoppingCart cart);
}
