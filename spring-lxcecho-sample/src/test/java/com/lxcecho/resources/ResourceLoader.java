package com.lxcecho.resources;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.Resource;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class ResourceLoader {

	@Test
	public void demo1() {
		ApplicationContext context = new ClassPathXmlApplicationContext();
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
