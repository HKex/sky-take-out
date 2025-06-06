package com.sky.controller.user;

import com.sky.entity.Category;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userCategoryController")
@RequestMapping("/user/category")
@Api(tags = "C端分类接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 分类查询
     * @param type
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("分类查询")
    public Result<List<Category>> list(Long type){
        List<Category> list = categoryService.getByListType(type);
        return Result.success(list);
    }

}
