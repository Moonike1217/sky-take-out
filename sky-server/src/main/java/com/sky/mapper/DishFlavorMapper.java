package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 批量插入菜品风味
     * @param dishFlavors
     */
    void insertBatch(List<DishFlavor> dishFlavors);

    /**
     * 根据菜品id批量删除风味
     * @param ids
     */
    void deleteByDishIds(List<Long> ids);

    /**
     * 修改菜品风味
     * @param flavors
     */
    void updateFlavor(List<DishFlavor> flavors);

    /**
     * 根据菜品id查询菜品风味
     * @param id
     * @return
     */
    @Select("select * from sky_take_out.dish_flavor where dish_id = #{id};")
    List<DishFlavor> getByDishId(Long id);
}
