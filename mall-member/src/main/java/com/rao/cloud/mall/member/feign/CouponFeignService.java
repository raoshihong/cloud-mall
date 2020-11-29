package com.rao.cloud.mall.member.feign;

import com.rao.cloud.mall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 使用openFeign声明式调用
 * @author raoshihong
 * @date 2020-11-04 18:50
 *
 *
 * @FeignClient 指定远程调用哪个应用服务,这个服务的名称也就是注册中心中的名称
 */
@FeignClient("mall-coupon")
public interface CouponFeignService {

    /**
     * 这里定义远程调用接口声明即可,这个方法的签名与CouponControler中定义的一致
     * @return
     */
    @RequestMapping("/coupon/coupon/member/list")
    R memberCoupons();
}
