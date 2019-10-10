package com.atlwj.demo.web;

import com.atlwj.demo.web.config.WebConfig;
import com.atlwj.demo.web.config.server.ContainerFactory;
import com.atlwj.demo.web.config.server.WebContainer;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.File;


public class MyApp {

	public static void main(String[] args) {
		//run("tomcat", 8081, "/lengwj-web01");
		run2();
	}

	private static void run2(){
		// 创建web上下文
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(WebConfig.class);
		context.refresh();


		File base = new File(System.getProperty("java.io.tmpdir"));
		Tomcat tomcat = new Tomcat();
		tomcat.setPort(9090);
		// 添加的不是web项目
		Context ctx = tomcat.addContext("/lengwj-web02", base.getAbsolutePath());

		DispatcherServlet dispatcherServlet = new DispatcherServlet(context);
		Tomcat.addServlet(ctx,"dispatcherServlet",dispatcherServlet).setLoadOnStartup(1);
		ctx.addServletMappingDecoded("/","dispatcherServlet");
		try {
			tomcat.start();
			tomcat.getServer().await();
		} catch (LifecycleException e) {
			e.printStackTrace();
		}
	}

	private static void run(String containerName, Integer port, String contextPath) {
		WebContainer container = ContainerFactory.getContainer(containerName, port, contextPath);
		container.run();
	}

}
