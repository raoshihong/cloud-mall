package com.rao.cloud.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rao.cloud.mall.common.utils.PageUtils;
import com.rao.cloud.mall.coupon.entity.SpuBoundsEntity;

import java.util.Map;

/**
 * ��Ʒspu��������
 *
 * @author raoshihong
 * @email raoshihong@gmail.com
 * @date 2020-11-03 19:07:16
 */
public interface SpuBoundsService extends IService<SpuBoundsEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

