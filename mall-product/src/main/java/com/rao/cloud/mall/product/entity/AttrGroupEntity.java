package com.rao.cloud.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * ���Է���
 * 
 * @author raoshihong
 * @email raoshihong@gmail.com
 * @date 2020-11-03 17:33:32
 */
@Data
@TableName("pms_attr_group")
public class AttrGroupEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ����id
	 */
	@TableId
	private Long attrGroupId;
	/**
	 * ����
	 */
	private String attrGroupName;
	/**
	 * ����
	 */
	private Integer sort;
	/**
	 * ����
	 */
	private String descript;
	/**
	 * ��ͼ��
	 */
	private String icon;
	/**
	 * ��������id
	 */
	private Long catelogId;

}
