package com.spring.research;

import com.spring.research.config.Config;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;

import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletException;
import java.io.File;

public class ResearchApplication {
		public static void main(String[] args) throws ServletException, LifecycleException {
			Tomcat tomcat = new Tomcat();
			tomcat.setPort(8083);
			tomcat.getConnector();
			//获取指定的绝对路径获取上下文
			Context context = tomcat.addWebapp("/", new File("src/main/webapp").getAbsolutePath());
			//此处可通过context设置servlet和listener.
			WebResourceRoot root = new StandardRoot(context);
			//Note:gradle项目是build/classes，maven项目是target/classes，踩了坑
			String path = new File("build/classes").getAbsolutePath();
			System.out.println(path);
     		root.addPreResources(new DirResourceSet(root, "/WEB-INF/classes", path, "/"));
			context.setResources(root);
			tomcat.start();
			tomcat.getServer().await();
	}
}
