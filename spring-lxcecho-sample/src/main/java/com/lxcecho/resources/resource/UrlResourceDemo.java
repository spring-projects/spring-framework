package com.lxcecho.resources.resource;

import org.springframework.core.io.UrlResource;

import java.net.MalformedURLException;

/**
 * 演示UrlResource访问网络资源
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class UrlResourceDemo {

	public static void main(String[] args) {
		//http前缀
//        loadUrlResource("http://www.baidu.com");

		//file前缀
		loadUrlResource("file:lxcecho.txt");
	}

	//访问前缀http、file
	public static void loadUrlResource(String path) {

		try {
			//创建Resource实现类的对象 UrlResource
			UrlResource url = new UrlResource(path);

			//获取资源信息
			System.out.println(url.getFilename());
			System.out.println(url.getURI());
			System.out.println(url.getDescription());
			System.out.println(url.getInputStream().read());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
