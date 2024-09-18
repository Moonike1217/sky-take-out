package com.sky.mapper;

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
}
