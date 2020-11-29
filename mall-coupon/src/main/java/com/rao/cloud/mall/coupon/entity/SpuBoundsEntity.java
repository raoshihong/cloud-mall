package com.rao.cloud.mall.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * ��Ʒspu��������
 * 
 * @author raoshihong
 * @email raoshihong@gmail.com
 * @date 2020-11-03 19:07:16
 */
@Data
@TableName("sms_spu_bounds")
public class SpuBoundsEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	private Long id;
	/**
	 * 
	 */
	private Long spuId;
	/**
	 * �ɳ�����
	 */
	private BigDecimal growBounds;
	/**
	 * �������
	 */
	private BigDecimal buyBounds;
	/**
	 * �Ż���Ч���[1111���ĸ�״̬λ�����ҵ���;0 - ���Żݣ��ɳ������Ƿ�����;1 - ���Żݣ���������Ƿ�����;2 - ���Żݣ��ɳ������Ƿ�����;3 - ���Żݣ���������Ƿ����͡�״̬λ0�������ͣ�1�����͡�]
	 */
	private Integer work;

}
