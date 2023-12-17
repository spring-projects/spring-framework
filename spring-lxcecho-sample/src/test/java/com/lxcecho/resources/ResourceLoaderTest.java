package com.lxcecho.resources;

import com.lxcecho.resources.resourceloaderaware.TestResourceLoaderAwareBean;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * ResourceLoader：该接口实现类的实例可以获得一个 Resource 实例
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class ResourceLoaderTest {

	@Test
	public void testOtherResource() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext();
		// 通过 ApplicationContext 访问资源：ApplicationContext 实例获取 Resource 实例时，默认采用与 ApplicationContext 相同的资源访问策略
		Resource res = ctx.getResource("lxcecho.txt");
		System.out.println("==" + res);
		System.out.println(res.getFilename());
	}

	/**
	 * ClassPathXmlApplicationContext 获取 Resource 实例
	 */
	@Test
	public void testResourceLoader01() {
		ApplicationContext context = new ClassPathXmlApplicationContext();
		// 该接口仅有这个方法，用于返回一个 Resource 实例
		Resource resource = context.getResource("lxcecho.txt");
		System.out.println(resource.getFilename());
	}

	/**
	 * FileSystemApplicationContext 获取 Resource 实例
	 */
	@Test
	public void testResourceLoader02() {
		ApplicationContext context = new FileSystemXmlApplicationContext();
		Resource resource = context.getResource("lxcecho.txt");
		System.out.println(resource.getFilename());
	}

	@Test
	public void testResourceLoaderAware() {
		// Spring 容器会将一个 ResourceLoader 对象作为该方法的参数传入
		ApplicationContext context = new ClassPathXmlApplicationContext("bean-resource.xml");
		TestResourceLoaderAwareBean testResourceLoaderAwareBean = context.getBean("testResourceLoaderAwareBean", TestResourceLoaderAwareBean.class);
		// 获取 ResourceLoader 对象
		ResourceLoader resourceLoader = testResourceLoaderAwareBean.getResourceLoader();
		System.out.println("Spring 容器将自身注入到 ResourceLoaderAware Bean 中 ？ ：" + (resourceLoader == context));

		// 加载其他资源
		Resource resource = resourceLoader.getResource("lxcecho.txt");
		System.out.println(resource.getFilename());
		System.out.println(resource.getDescription());
	}

}
