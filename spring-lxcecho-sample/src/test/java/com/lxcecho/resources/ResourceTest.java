package com.lxcecho.resources;

import com.lxcecho.resources.di.ResourceBean;
import com.lxcecho.resources.prefix.User;
import com.lxcecho.resources.resource.ClassPathResourceDemo;
import com.lxcecho.resources.resource.FileSystemResourceDemo;
import com.lxcecho.resources.resource.UrlResourceDemo;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.Resource;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/17
 */
public class ResourceTest {

	@Test
	public void testUrlResource() {
		// http 前缀：访问网络资源
//		UrlResourceDemo.loadUrlResource("http://www.baidu.com");

		// file 前缀：访问文件系统资源
		UrlResourceDemo.loadUrlResource("file:url-file.txt");  // 项目根目录下的文件
	}

	@Test
	public void testClassPathResource() {
		ClassPathResourceDemo.loadClasspathResource("lxcecho.txt");
	}

	@Test
	public void testFileSystemResource() {
		FileSystemResourceDemo.loadFileResource("D:\\lxcecho.txt");
		FileSystemResourceDemo.loadFileResource("url-file.txt");
	}

	@Test
	public void testResourceDI() {
		ApplicationContext context = new ClassPathXmlApplicationContext("bean-resource.xml");
		ResourceBean resourceBean = context.getBean(ResourceBean.class);
		resourceBean.parse();
	}

	/**
	 * ApplicationContext 确定资源访问策略通常有两种方法：
	 * （1）使用 ApplicationContext 实现类指定访问策略。
	 * （2）使用前缀指定访问策略。
	 */
	@Test
	public void testPrefix() {
		/**
		 * 使用 classpath: 前缀指定访问策略
		 * 通过搜索文件系统路径下的 xml 文件创建 ApplicationContext，但通过指定 classpath: 前缀强制搜索类加载路径：classpath:bean-resource.xml
		 * */
		/*ApplicationContext context = new ClassPathXmlApplicationContext("classpath:bean-resource.xml");
		System.out.println(context);
        Resource resource = context.getResource("lxcecho.txt");
        System.out.println(resource.getDescription());

		User user = context.getBean(User.class);
		System.out.println(user);*/

		/**
		 * classpath 通配符使用
		 * classpath*: 前缀提供了加载多个 XML 配置文件的能力，当使用 classpath*: 前缀来指定 XML 配置文件时，系统将搜索类加载路径，找到所有与文件名匹配的文件，分别加载文件中的配置定义，最后合并成一个 ApplicationContext。
		 * 当使用classpath * :前缀时，Spring将会搜索类加载路径下所有满足该规则的配置文件。
		 *
		 * 如果不是采用 classpath*: 前缀，而是改为使用 classpath: 前缀，Spring 则只加载第一个符合条件的 XML 文件
		 */
		// Spring 允许将 classpath*: 前缀和通配符结合使用
		ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath*:bean-*.xml");
		System.out.println(ctx);
	}

}
