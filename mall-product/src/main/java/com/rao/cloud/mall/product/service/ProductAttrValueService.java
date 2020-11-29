package com.rao.cloud.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rao.cloud.mall.common.utils.PageUtils;
import com.rao.cloud.mall.product.entity.ProductAttrValueEntity;

import java.util.Map;

/**
 * spu����ֵ
 *
 * @author raoshihong
 * @email raoshihong@gmail.com
 * @date 2020-11-03 17:33:32
 */
public interface ProductAttrValueService extends IService<ProductAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

