package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品及其风味
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

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断 - 菜品是否已经处于停售状态
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {
                //菜品处于销售状态
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判断 - 菜品是否关联其他套餐
        List<Long> setmealIds = setmealDishMapper.getSetmealIdByDishIds(ids);
        if (setmealIds != null && !setmealIds.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除菜品
        dishMapper.deleteByIds(ids);

        //删除菜品对应风味
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据id查询对应菜品
     * @param id
     * @return
     */
    @Override
    public DishVO getById(String id) {
        Dish dish = dishMapper.getById(Long.valueOf(id));
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        return dishVO;
    }

    /**
     * 修改菜品
     * @param dishDTO
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        //拷贝菜品基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //拷贝菜品风味信息
        List<DishFlavor> dishFlavors = dishDTO.getFlavors();

        //修改菜品基本信息
        dishMapper.update(dish);

        //得到修改的菜品信息的主键值(id)
        Long id = dish.getId();

        //遍历风味列表,为每个flavor列表的dishId赋值
        dishFlavors.forEach(dishFlavor -> {
            dishFlavor.setDishId(id);
        });

        //批量插入(插入 n 条风味信息)
        dishFlavorMapper.insertBatch(dishFlavors);
    }

    @Override
    public void updateStatus(String id, String status) {
        Dish dish = new Dish();
        dish.setId(Long.valueOf(id));
        dish.setStatus(Integer.valueOf(status));
        dishMapper.update(dish);
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> getByCategoryId(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
