package com.lxcecho;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

/**
 * 怎么启动起来？
 * Tomcat启动
 * SPI机制下 QuickAppStarter生效创建 ioc容器配置DispatcherServlet等各种组件
 * <p>
 * 导入各种starter依赖，SpringBoot封装了很多的自动配置，帮我们给容器中放了很多组件。
 * SpringBoot封装了功能的自动配置
 * <p>
 * WebServerFactory做到了
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2024/1/1
 */
public class Main {
	public static void main(String[] args) throws LifecycleException {
		//自己写Tomcat的启动源码
		Tomcat tomcat = new Tomcat();


		tomcat.setPort(8888);
		tomcat.setHostname("localhost");
		tomcat.setBaseDir(".");

		Context context = tomcat.addWebapp("/boot", System.getProperty("user.dir") + "/src/main");


//        DispatcherServlet servlet = new DispatcherServlet();
		//给Tomcat里面添加一个Servlet
//        Wrapper hello = tomcat.addServlet("/boot", "hello", new HelloServlet());
//        Wrapper hello = tomcat.addServlet("/boot", "hello", servlet);
//        hello.addMapping("/"); //指定处理的请求

		//自己创建 DispatcherServlet 对象，并且创建ioc容器，DispatcherServlet里面有ioc容器

		//自己创建一个DispatcherServlet注册进去
//        tomcat.addServlet(自己创建一个DispatcherServlet注册进去)


		tomcat.start();//启动tomcat 注解版MVC利用Tomcat SPI机制


		tomcat.getServer().await(); //服务器等待

	}
}