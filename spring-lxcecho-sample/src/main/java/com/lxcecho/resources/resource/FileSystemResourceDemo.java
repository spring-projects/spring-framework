package com.lxcecho.resources.resource;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * 访问系统资源
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class FileSystemResourceDemo {

	/**
	 * FileSystemResource 实例可使用 FileSystemResource 构造器显示地创建，但更多的时候它都是隐式创建。
	 * 执行 Spring 的某个方法时，该方法接受一个代表资源路径的字符串参数，当 Spring 识别该字符串参数中包含 file: 前缀后，系统将会自动创建 FileSystemResource 对象。
	 *
	 * @param path 相对路径/绝对路径
	 */
	public static void loadFileResource(String path) {
		// 创建对象
		FileSystemResource resource = new FileSystemResource(path);
		// 获取文件名
		System.out.println(resource.getFilename());
		// 获取文件描述
		System.out.println(resource.getDescription());
		//获取文件内容
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
