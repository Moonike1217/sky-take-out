package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 在用户表中添加新用户
     * @param user
     */
    void insert(User user);

    /**
     * 根据主键id查询用户
     * @param id
     * @return
     */
    @Select("select * from user where id = #{id}")
    User getById(Long id);
}
