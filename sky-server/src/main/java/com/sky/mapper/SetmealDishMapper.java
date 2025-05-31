package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询套餐id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 批量插入
     * @param dishes
     */
    void insertBatch(List<SetmealDish> dishes);

    /**
     * 删除套餐和菜品的关联关系
     * @param ids
     */
    void deleteBySetmealId(List<Long> ids);

    /**
     * 根据套餐id查询套餐的菜品
     * @param setmealId
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getDishBySetmealId(Long setmealId);
}
