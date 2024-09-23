package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品id列表，在套餐-菜品表中返回对应的套餐id
     * @param ids
     * @return
     */
    List<Long> getSetmealIdByDishIds(List<Long> ids);

    /**
     * 添加套餐
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐id，删除套餐-菜品表中的对应的数据项
     * @param ids
     */
    void deleteBySetmealId(List<Long> ids);
}
