package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
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

    @Autowired
    private DishMapper dishMapper;

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

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    public SetmealVO getByIdWithDish(Long id) {
        //获取套餐
        Setmeal setmeal = setmealMapper.getById(id);

        //获取套餐下菜品
        List<SetmealDish> list = setmealDishMapper.getDishBySetmealId(setmeal.getId());

        //返回VO
        SetmealVO VO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,VO);
        VO.setSetmealDishes(list);

        return VO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        //更新套餐数据
        setmealMapper.update(setmeal);

        Long id = setmealDTO.getId();

        //删除老套餐菜品数据
        setmealDishMapper.deleteBySetmealId(List.of(id));

        //插入新数据
        List<SetmealDish> dishes = setmealDTO.getSetmealDishes();
        dishes.forEach(dish ->{
            dish.setSetmealId(id);
        });
        setmealDishMapper.insertBatch(dishes);
    }

    /**
     * 启售停售套餐
     * @param status
     * @param id
     */
    public void startOrstop(Integer status, Long id) {
        //起售套餐中不能有未起售菜品
        if(status == StatusConstant.ENABLE){
            List<Dish> dishes = dishMapper.getBySetmealId(id);
            if(dishes != null && dishes.size() > 0){
                dishes.forEach(dish ->{
                    if (dish.getStatus().equals(StatusConstant.DISABLE)){
                        throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }

        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
