package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> result = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(result.getTotal(),result.getResult());
    }

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        //插入Setmeal中
        setmealMapper.insert(setmeal);
        Long setmealId = setmeal.getId();

        //获取DTO中的SetmealDish
        List<SetmealDish> dishes = setmealDTO.getSetmealDishes();
        dishes.forEach(dish ->{
            dish.setSetmealId(setmealId);
        });

        //插入到SetmealDish中
        setmealDishMapper.insertBatch(dishes);
    }

    /**
     * 删除套餐
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //没有发售的套餐才可以删除
        ids.forEach(id ->{
            Setmeal setmeal = setmealMapper.getById(id);
            if(setmeal.getStatus().equals(StatusConstant.ENABLE)){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        //删除套餐表中的套餐数据
        setmealMapper.deleteBatch(ids);
        //删除SetmealDish中的数据
        setmealDishMapper.deleteBySetmealId(ids);
    }
}
