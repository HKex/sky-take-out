package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;

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
}
