package com.rao.cloud.mall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rao.cloud.mall.common.utils.PageUtils;
import com.rao.cloud.mall.order.entity.PaymentInfoEntity;

import java.util.Map;

/**
 * ֧����Ϣ��
 *
 * @author raoshihong
 * @email raoshihong@gmail.com
 * @date 2020-11-03 18:10:16
 */
public interface PaymentInfoService extends IService<PaymentInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

