package com.lxcecho.resources.resource;

import org.springframework.core.io.UrlResource;

import java.net.MalformedURLException;

/**
 * 演示 UrlResource 访问网络资源
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class UrlResourceDemo {

	public static void main(String[] args) {
		// http 前缀
//        loadUrlResource("http://www.baidu.com");

		// file 前缀
		loadUrlResource("file:settings.gradle");
	}

	/**
	 * 访问前缀 http、file
	 *
	 * @param path
	 */
	public static void loadUrlResource(String path) {

		try {
			// 创建 Resource 实现类的对象 UrlResource
			UrlResource url = new UrlResource(path);

			// 获取资源信息
			System.out.println(url.getFilename());
			System.out.println(url.getURI());
			System.out.println(url.getDescription());
			System.out.println(url.getInputStream().read());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
