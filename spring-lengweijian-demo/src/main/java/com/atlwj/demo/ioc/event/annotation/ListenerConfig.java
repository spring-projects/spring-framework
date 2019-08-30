package com.atlwj.demo.ioc.event.annotation;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.atlwj.demo.ioc.event.annotation")
public class ListenerConfig {


	private static final String BLACK_LIST_01 = "known.hacker@example.org";

	private static final String BLACK_LIST_02 = "known.spammer@example.org";

	private static final String BLACK_LIST_03 = "john.doe@example.org";

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ioc = new AnnotationConfigApplicationContext(ListenerConfig.class);
		EmailService bean = ioc.getBean(EmailService.class);

		// 如果邮件内容以"亲爱的"开头，就不用充钱。
		//bean.sendEmail(BLACK_LIST_03,"老铁，你好！");
		bean.sendEmail(BLACK_LIST_03,"sss亲爱的sss，不要嘛！");

	}


}
