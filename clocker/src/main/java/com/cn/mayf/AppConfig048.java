package com.cn.mayf;

import com.cn.mayf.depenteach.DepentService02;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * @Author mayf
 * @Date 2021/3/30 23:04
 */
@ComponentScan("com.cn.mayf.depenteach")
public class AppConfig048 {

	@Bean
	public DepentService02 getDepencyService02(){
		return new DepentService02();
	}
}
