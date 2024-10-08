package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 菜品口味
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishFlavor implements Serializable {

    private static final long serialVersionUID = 1L;

    //主键
    private Long id;

    //菜品id(对应菜品表中的id列)
    private Long dishId;

    //口味名称
    private String name;

    //口味数据list
    private String value;

}
