package com.rao.cloud.mall.member;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 *
 * 使用nacos注册中心
 * 1. 引入nacos注册中心依赖
 *  <dependency>
 *             <groupId>com.alibaba.cloud</groupId>
 *             <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
 *         </dependency>
 *
 * 2. 配置nacos注册中心配置
 * spring:
 *    cloud:
 *     nacos:
 *       discovery:
 *         server-addr: 127.0.0.1:8848
 *
 *   application:
 *     name: mall-member
 *
 * 3. 开启服务发现客户端注册
 *      @EnableDiscoveryClient
 *
 * 使用OpenFeign
 *
 * 1.引入OpenFeign依赖
 *          <dependency>
 *             <groupId>org.springframework.cloud</groupId>
 *             <artifactId>spring-cloud-starter-openfeign</artifactId>
 *         </dependency>
 * 2.创建OpenFeign客户端调用接口
 *      com.rao.cloud.mall.member.feign.CouponFeignService
 *      使用@FeignClient("mall-coupon")指定服务提供者应用名称
 *
 * 3.开启feign客户端
 *      @EnableFeignClients
 *
 *
 * 使用nacos配置中心
 * 1.引入nacos配置中心依赖
 *      <dependency>
 *             <groupId>com.alibaba.cloud</groupId>
 *             <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
 *         </dependency>
 * 2. 创建bootstrap.properties文件
 *      配置nacos配置中心地址和对应应用的配置信息
 *
 *
 *
 *
 */
@EnableFeignClients
@EnableDiscoveryClient
@MapperScan("com.rao.cloud.mall.member.dao")
@SpringBootApplication
public class MallMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallMemberApplication.class, args);
    }

}
