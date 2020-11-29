package com.rao.cloud.mall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rao.cloud.mall.common.utils.PageUtils;
import com.rao.cloud.mall.member.entity.MemberStatisticsInfoEntity;

import java.util.Map;

/**
 * ��Աͳ����Ϣ
 *
 * @author raoshihong
 * @email raoshihong@gmail.com
 * @date 2020-11-03 19:03:51
 */
public interface MemberStatisticsInfoService extends IService<MemberStatisticsInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

