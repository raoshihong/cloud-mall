package com.rao.cloud.mall.mallproduct;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rao.cloud.mall.product.MallProductApplication;
import com.rao.cloud.mall.product.entity.BrandEntity;
import com.rao.cloud.mall.product.service.BrandService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = {MallProductApplication.class})
class MallProductApplicationTests {

	@Autowired
	private BrandService brandService;

	@Test
	void contextLoads() {

		BrandEntity brandEntity = new BrandEntity();
		brandEntity.setName("AA品牌");
		brandService.save(brandEntity);

		brandEntity.setDescript("aaaa");
		brandService.updateById(brandEntity);

		LambdaQueryWrapper<BrandEntity> queryWrapper = Wrappers.lambdaQuery();
		queryWrapper.eq(BrandEntity::getName,"AA品牌");
		List<BrandEntity> brandEntityList = brandService.list(queryWrapper);

		System.out.println(brandEntityList);
	}

}
