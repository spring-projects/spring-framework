package com.zxc.learn;

import com.zxc.learn.bean.Config;
import com.zxc.learn.custom.MyBeanNameGenerator;
import com.zxc.learn.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author simon
 * @version 1.0.0
 * @description:
 * @create: 2019-02-24 19:24
 **/
@Slf4j
public class App {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		UserService service = context.getBean(UserService.class);
		log.info(service.toString());
		service.add("DASFSA");
	}
}
