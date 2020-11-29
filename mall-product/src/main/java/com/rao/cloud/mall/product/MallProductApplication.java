package com.rao.cloud.mall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 1.引入mybatis-plus依赖
 * 	<dependency>
 *             <groupId>com.baomidou</groupId>
 *             <artifactId>mybatis-plus-boot-starter</artifactId>
 *             <version>3.4.0</version>
 *         </dependency>
 * 2.引入mysql依赖
 * <dependency>
 *             <groupId>mysql</groupId>
 *             <artifactId>mysql-connector-java</artifactId>
 *             <version>8.0.17</version>
 *         </dependency>
 *
 * 3.配置mybatis和数据源相关配置
 * 		在启动类上指定mapper类的位置@MapperScan("com.rao.cloud.mall.product.dao")
 * 		在application.yml中配置
 * 			spring:
 *   datasource:
 *     username: root
 *     password: root
 *     url: jdbc:mysql://192.168.8.101:3306/mall_pms?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
 *     driver-class-name: com.mysql.jdbc.Driver
 *	mybatis-plus:
 *
 *
 * 4. 添加@EnableDiscoveryClient,开启服务注册中心
 */
@EnableDiscoveryClient
@MapperScan("com.rao.cloud.mall.product.dao")
@SpringBootApplication
public class MallProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(MallProductApplication.class, args);
	}

}
