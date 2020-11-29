package com.rao.cloud.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * ֧����Ϣ��
 * 
 * @author raoshihong
 * @email raoshihong@gmail.com
 * @date 2020-11-03 18:10:16
 */
@Data
@TableName("oms_payment_info")
public class PaymentInfoEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	private Long id;
	/**
	 * �����ţ�����ҵ��ţ�
	 */
	private String orderSn;
	/**
	 * ����id
	 */
	private Long orderId;
	/**
	 * ֧����������ˮ��
	 */
	private String alipayTradeNo;
	/**
	 * ֧���ܽ��
	 */
	private BigDecimal totalAmount;
	/**
	 * ��������
	 */
	private String subject;
	/**
	 * ֧��״̬
	 */
	private String paymentStatus;
	/**
	 * ����ʱ��
	 */
	private Date createTime;
	/**
	 * ȷ��ʱ��
	 */
	private Date confirmTime;
	/**
	 * �ص�����
	 */
	private String callbackContent;
	/**
	 * �ص�ʱ��
	 */
	private Date callbackTime;

}
