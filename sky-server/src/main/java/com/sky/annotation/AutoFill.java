package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD) //指定注解的作用对象是方法
public @interface AutoFill {
    //指定当前数据库的操作类型:UPDATE INSERT
    OperationType value(); //定义了一个注解的属性
}
