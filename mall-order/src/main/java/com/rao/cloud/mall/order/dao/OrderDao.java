package com.rao.cloud.mall.order.dao;

import com.rao.cloud.mall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * ����
 * 
 * @author raoshihong
 * @email raoshihong@gmail.com
 * @date 2020-11-03 18:10:16
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
