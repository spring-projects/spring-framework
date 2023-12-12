package com.lxcecho.junit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
//@ExtendWith(SpringExtension.class)
//@ContextConfiguration("classpath:bean.xml")
@SpringJUnitConfig(locations = "classpath:bean.xml")
public class SpringTestJunit5 {

	//注入
	@Autowired
	private User user;

	//测试方法
	@Test
	public void testUser() {
		System.out.println(user);
		user.run();
	}

}

@Component
class User {

	public void run() {
		System.out.println("user.....");
	}
}
