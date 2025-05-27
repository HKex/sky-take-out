package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Insert("INSERT INTO category(type, name, sort, status, create_time, update_time, create_user, update_user) " +
            "VALUES " +
            "(#{type},#{name},#{sort},#{status},#{createTime},#{updateTime},#{createUser},#{updateUser})")
    void insert(Category category);

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 根据id动态修改属性
     * @param category
     */
    void update(Category category);

    /**
     * 根据list类型查询
     * @param id
     * @return
     */
    List<Category> getByListType(Integer id);

    /**
     * 根据id删除分类
     * @param id
     */
    @Delete("DELETE from category where id = #{id}")
    int deleteById(Long id);
}
