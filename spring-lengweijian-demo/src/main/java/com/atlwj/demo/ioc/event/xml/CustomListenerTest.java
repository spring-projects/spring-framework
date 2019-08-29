package com.atlwj.demo.ioc.event.xml;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CustomListenerTest {

	private static final String BLACK_LIST_01 = "known.hacker@example.org";

	private static final String BLACK_LIST_02 = "known.spammer@example.org";

	private static final String BLACK_LIST_03 = "john.doe@example.org";

	public static void main(String[] args) {
		ClassPathXmlApplicationContext ioc = new ClassPathXmlApplicationContext("listener.xml");
		EmailService bean = (EmailService) ioc.getBean("emailService");
		bean.sendEmail(BLACK_LIST_01,"hello man!!It's me");
	}
}
