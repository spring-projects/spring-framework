package com.lxcecho.resources;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.Resource;

/**
 * ResourceLoader：该接口实现类的实例可以获得一个 Resource 实例
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class ResourceLoaderTest {

	@Test
	public void demo1() {
		ApplicationContext context = new ClassPathXmlApplicationContext();
		// 该接口仅有这个方法，用于返回一个 Resource 实例
		Resource resource = context.getResource("lxcecho.txt");
		System.out.println(resource.getFilename());
	}

	@Test
	public void demo2() {
		ApplicationContext context = new FileSystemXmlApplicationContext();
		Resource resource = context.getResource("lxcecho.txt");
		System.out.println(resource.getFilename());
	}

}
