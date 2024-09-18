package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    /**
     * 新增菜品
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        //将除了风味列表以外的部分保存下来
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //保存风味列表
        List<DishFlavor> dishFlavors = dishDTO.getFlavors();

        //插入1条菜品信息
        dishMapper.insert(dish);

        //得到插入的菜品信息的主键值(id)
        Long id = dish.getId();

        //遍历风味列表,为每个flavor列表的dishId赋值
        dishFlavors.forEach(dishFlavor -> {
            dishFlavor.setDishId(id);
        });

        //批量插入(插入 n 条风味信息)
        dishFlavorMapper.insertBatch(dishFlavors);
    }
}
