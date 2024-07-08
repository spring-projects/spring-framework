package org.springframework.aop;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author sushuaiqiang
 * @date 2024/7/8 - 11:13
 */
public class Main {
	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("aop/aopBean.xml");
		AopBean bean = (AopBean) context.getBean("test");
		bean.test();
	}
}
