package com.bat.spring.test;

import com.bat.spring.entity.User;
import com.bat.spring.service.UserService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @program: spring source code
 * @author: zhq
 * @description: attack bat
 * @create: 2021/9/2 16:08
 **/
public class BatTest {
	public static void main(String[] args) {
		//https://github.com/zhangxiansheng123/spring-framework.git
		//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
		//https://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@github.com/zhangxiansheng123/spring-framework.git
		//Spring的HelloWorld就是这几行代码
		//先思考两个问题:1、容器创建时做了什么? 2、getBean()时又做了什么?

		//读取spring配置文件
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-test.xml");

		UserService userService = (UserService) context.getBean("userService");

		User user = userService.getUserById(1);

		System.out.println("User = " + user);

	}
}
