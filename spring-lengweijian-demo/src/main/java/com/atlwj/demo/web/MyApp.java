package com.atlwj.demo.web;

import com.atlwj.demo.web.servlet.HelloServlet;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.springframework.web.WebApplicationInitializer;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;


public class MyApp {
	public static void main(String[] args) {
		try {
			MyApp.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void run() throws LifecycleException {
		// 创建Tomcat应用对象
		Tomcat tomcat = new Tomcat();

		// 设置Tomcat的端口号
		tomcat.setPort(8080);
		// 是否设置Tomcat自动部署
		//tomcat.getHost().setAutoDeploy(false);
		// 创建上下文
		StandardContext standardContext = new StandardContext();
		// 设置项目名
		standardContext.setPath("/lengwjweb");
		// 监听上下文
		standardContext.addLifecycleListener(new Tomcat.FixContextListener());
		// 向tomcat容器对象添加上下文配置
		tomcat.getHost().addChild(standardContext);
		String sourcePath = MyApp.class.getResource("/").getPath();
		Context ctx = tomcat.addWebapp("/", new File("src/main/webapp").getAbsolutePath());
		WebResourceRoot resource = new StandardRoot(ctx);
		resource.addPreResources(new DirResourceSet(resource,"/WEB-INF/classes",sourcePath,"/"));
		// 创建Servlet
		//tomcat.addServlet("", "coreServlet", new HelloServlet());
		// Servlet映射
		//standardContext.addServletMappingDecoded("/core", "coreServlet");

		//启动tomcat容器
		tomcat.start();
		//等待
		tomcat.getServer().await();
	}

}
