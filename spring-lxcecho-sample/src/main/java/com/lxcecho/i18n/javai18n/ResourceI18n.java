package com.lxcecho.i18n.javai18n;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class ResourceI18n {

	public static void main(String[] args) {
		ResourceBundle bundle1 = ResourceBundle.getBundle("messages",
				new Locale("zh", "CN"));
		String value1 = bundle1.getString("test");
		System.out.println(value1);

		ResourceBundle bundle2 = ResourceBundle.getBundle("messages",
				new Locale("en", "GB"));
		String value2 = bundle2.getString("test");
		System.out.println(value2);
	}
}
