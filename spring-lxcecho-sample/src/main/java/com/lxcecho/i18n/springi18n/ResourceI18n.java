package com.lxcecho.i18n.springi18n;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class ResourceI18n {

	public static void main(String[] args) {
		ApplicationContext context =
				new ClassPathXmlApplicationContext("bean.xml");

		Object[] objs = new Object[]{"lxcecho", new Date().toString()};
		String value = context.getMessage("www.lxcecho.com", objs, Locale.UK);
		System.out.println(value);
	}
}
