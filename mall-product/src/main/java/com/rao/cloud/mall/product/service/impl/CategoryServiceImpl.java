package com.rao.cloud.mall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rao.cloud.mall.common.utils.PageUtils;
import com.rao.cloud.mall.common.utils.Query;

import com.rao.cloud.mall.product.dao.CategoryDao;
import com.rao.cloud.mall.product.entity.CategoryEntity;
import com.rao.cloud.mall.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> tree() {

        List<CategoryEntity> categoryEntities = this.list();

        List<CategoryEntity> levelCategories =
            categoryEntities.stream().filter(categoryEntity -> categoryEntity.getParentCid() == 0).collect(Collectors.toList());
        levelCategories.forEach(categoryEntity -> {
            categoryEntity.setChildren(getChildren(categoryEntity,categoryEntities));
        });

        return levelCategories;
    }

    @Override
    public void removeMenuByIds(List<Long> ids) {
        baseMapper.deleteBatchIds(ids);
    }

    private List<CategoryEntity> getChildren(CategoryEntity current,List<CategoryEntity> categoryEntities){
        List<CategoryEntity> categoryEntityList =
            categoryEntities.stream().filter(categoryEntity -> current.getCatId().equals(categoryEntity.getParentCid()))
                .collect(Collectors.toList());
        categoryEntityList.forEach(categoryEntity -> {
            categoryEntity.setChildren(getChildren(categoryEntity,categoryEntities));
        });
        return categoryEntityList;
    }

}