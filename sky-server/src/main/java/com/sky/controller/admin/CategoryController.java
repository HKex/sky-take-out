package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理
 */
@RestController
@RequestMapping("/admin/category")
@Slf4j
@Api(tags = "分类相关接口")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增分类
     * @param categoryDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增分类")
    public Result<String> save(@RequestBody CategoryDTO categoryDTO){
        log.info("新增分类：{}",categoryDTO);
        categoryService.save(categoryDTO);
        return Result.success();
    }

    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分页查询")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("分页查询：{}",categoryPageQueryDTO);
        PageResult result =  categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(result);
    }

    /**
     * 启用禁用分类
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public Result<String> startOrStop(@PathVariable Integer status, Long id){
        log.info("启用禁用分类：{}",id);
        categoryService.startOrStop(status,id);
        return Result.success();
    }

    /**
     * 根据分类类型查询
     * @param list
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类类型查询")
    public Result<List<Category>> getById(Integer list){
        List<Category> category = categoryService.getByListType(list);
        return Result.success(category);
    }

    //TODO
    public Result<String> deleteById(Long id){
        log.info("删除分类ID号：{}",id);
        categoryService.deleteById(id);
        return Result.success();
    }

    @PutMapping
    @ApiOperation("编辑分类")
    public Result<String> update(@RequestBody CategoryDTO categoryDTO){
        log.info("编辑分类：{}",categoryDTO);
        categoryService.update(categoryDTO);
        return Result.success();
    }
}
