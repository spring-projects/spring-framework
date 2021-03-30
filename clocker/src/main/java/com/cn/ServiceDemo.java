package com.cn;

import com.cn.mayf.service.BeanService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * @Author mayf
 * @Date 2021/3/7 16:56
 */
@Component
public class ServiceDemo {
	@Resource
	BeanService beanDemo;

	public ServiceDemo() {
		System.out.println("初始化Bean----"+ServiceDemo.class);
	}

	@PostConstruct
	public void test(){
		System.out.println(this.getClass().getSimpleName()+":这是一段PostConstruct测试");
	}

	@PreDestroy
	public void testDestroy(){
		System.out.println(this.getClass().getSimpleName()+":这是一段PreDestroy测试");
	}
}
