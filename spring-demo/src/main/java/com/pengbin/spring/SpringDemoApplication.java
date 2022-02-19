package com.pengbin.spring;

import com.pengbin.spring.config.SysConfig;
import com.pengbin.spring.pojo.SysUser;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SpringDemoApplication {
	public static void main(String[] args) {
		// 获取容器
		ApplicationContext ac =new AnnotationConfigApplicationContext(SysConfig.class);
		// 获取 bean
		SysUser user = (SysUser) ac.getBean("sysUser");
		System.out.println(user.toString());
	}
}
