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
		ApplicationContext context = new ClassPathXmlApplicationContext("bean-i18n.xml");

		// 传递动态参数，使用数组形式对应 {0} {1} 顺序
		Object[] objs = new Object[]{"lxcecho", new Date().toString()};

		// www.lxcecho.com 为资源文件的 key 值,
		//objs 为资源文件 value 值所需要的参数，Local.CHINA 为国际化为语言
		String str=context.getMessage("echo", objs, Locale.CHINA);
		System.out.println(str);
	}
}
