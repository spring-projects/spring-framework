package com.lxcecho.resources.resource;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * 访问类路径下资源
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class ClassPathResourceDemo {

	/**
	 * ClassPathResource 实例可使用 ClassPathResource 构造器显式地创建，但更多的时候它都是隐式地创建的。
	 * 当执行 Spring 的某个方法时，该方法接受一个代表资源路径的字符串参数，当 Spring 识别该字符串参数中包含 classpath: 前缀后，系统会自动创建 ClassPathResource 对象。
	 *
	 * @param path
	 */
	public static void loadClasspathResource(String path) {
		// 创建对象 ClassPathResource
		ClassPathResource resource = new ClassPathResource(path);

		System.out.println(resource.getFilename());
		System.out.println(resource.getDescription());
		// 获取文件内容
		try {
			InputStream in = resource.getInputStream();
			byte[] b = new byte[1024];
			while (in.read(b) != -1) {
				System.out.println(new String(b));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
