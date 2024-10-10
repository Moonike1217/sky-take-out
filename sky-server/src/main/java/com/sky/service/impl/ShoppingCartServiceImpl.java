package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
   private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增购物车
     * @param shoppingCartDTO
     */
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        //将DTO的属性拷贝到entity中(设置dishId SetmealId dishFlavors)
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        //判断添加的是菜品还是套餐(设置name amount image)
        Long dishId = shoppingCart.getDishId();
        Long SetmealId = shoppingCart.getSetmealId();

        if (dishId == null) {
            //添加的是套餐
            Setmeal setmeal = setmealMapper.getById(SetmealId);
            shoppingCart.setName(setmeal.getName());
            shoppingCart.setAmount(setmeal.getPrice());
            shoppingCart.setImage(setmeal.getImage());
        } else {
            //添加的是菜品
            Dish dish = dishMapper.getById(dishId);
            shoppingCart.setName(dish.getName());
            shoppingCart.setAmount(dish.getPrice());
            shoppingCart.setImage(dish.getImage());
        }

        //设置createTime UserId
        shoppingCart.setUserId(BaseContext.getCurrentId());
        shoppingCart.setCreateTime(LocalDateTime.now());

        //判断要添加的套餐或者菜品是否已经存在(设置number)
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list != null && !list.isEmpty()) {
            //套餐或菜品已存在，更新数量即可
            ShoppingCart item = list.get(0);
            item.setNumber(item.getNumber() + 1);
            shoppingCartMapper.updateNumber(item);
        } else {
            //套餐或菜品不存在，将新数据插入数据库中
            shoppingCart.setNumber(1);
            shoppingCartMapper.insert(shoppingCart);
        }

        return;
    }

    /**
     * 查询购物车
     */
    @Override
    public List<ShoppingCart> list() {
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(BaseContext.getCurrentId());
        return shoppingCartMapper.list(shoppingCart);
    }

    /**
     * 清空购物车
     */
    @Override
    public void delete() {
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());
        return;
    }

    /**
     * 减少购物车中商品的数量
     * @param shoppingCartDTO
     */
    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {

        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        ShoppingCart cart = shoppingCartMapper.list(shoppingCart).get(0);

        //取出数量
        Integer cartNumber = cart.getNumber();

        if (cartNumber == 1) {
            //减去一份后变为0，需要从购物车中删除该item
            shoppingCartMapper.deleteById(cart);
        } else {
            //减去一份后仍有>=1份，可以直接减
            cart.setNumber(cartNumber - 1);
            shoppingCartMapper.updateNumber(cart);
        }
    }

}
