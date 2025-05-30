package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 批量插入口味数据
     * @param flavors
     */
    void insertBatch(List<DishFlavor> flavors);

    /**
     * 批量删除ids关联口味
     * @param ids
     */
    void deleteByDishIdBatch(List<Long> ids);

    /**
     * 根据菜品id查询口味数据
     * @param dishId
     * @return
     */
    @Select("SELECT * FROM dish_flavor where dish_id = #{dishId}")
    List<DishFlavor> getByDishId(Long dishId);
}
