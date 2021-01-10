package com.rao.cloud.mall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.rao.cloud.mall.member.feign.CouponFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rao.cloud.mall.member.entity.MemberEntity;
import com.rao.cloud.mall.member.service.MemberService;
import com.rao.cloud.mall.common.utils.PageUtils;
import com.rao.cloud.mall.common.utils.R;



/**
 * ��Ա
 *
 * @author raoshihong
 * @email raoshihong@gmail.com
 * @date 2020-11-03 19:03:51
 */
@RefreshScope
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Value("${member.user.name}")
    private String name;

    @Value("${member.user.gender}")
    private Integer gender;

    @RequestMapping("/coupons")
    public R memberCoupons(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("lisi");
        memberEntity.setUsername(name);
        memberEntity.setGender(gender);
        return R.ok().put("member",memberEntity);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
