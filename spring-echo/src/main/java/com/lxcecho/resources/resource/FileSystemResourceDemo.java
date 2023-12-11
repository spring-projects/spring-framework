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

	public static void main(String[] args) {
		ApplicationContext ctx = new ClassPathXmlApplicationContext();
//        通过ApplicationContext访问资源
//        ApplicationContext实例获取Resource实例时，
//        默认采用与ApplicationContext相同的资源访问策略
		Resource res = ctx.getResource("lxcecho.txt");
		System.out.println("==" + res);
		System.out.println(res.getFilename());

//        loadFileResource("c:\\lxcecho.txt");

//        loadFileResource("lxcecho.txt");
	}

	public static void loadFileResource(String path) {
		//创建对象
		FileSystemResource resource = new FileSystemResource(path);

		System.out.println(resource.getFilename());
		System.out.println(resource.getDescription());
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
