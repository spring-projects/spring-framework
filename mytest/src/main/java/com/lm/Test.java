package com.lm;

import com.lm.config.SpringConfig;
import com.lm.dao.UserDao;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Daniel
 * @Description
 * @Date 2020/6/14 0:39
 **/

public class Test {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(SpringConfig.class);
		UserDao dao = (UserDao) annotationConfigApplicationContext.getBean("dao");
		dao.pringInfo();

	}

}
