package com.spring.research.embed.tomcat.config;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;

public class EmbedTomcatApplication {
	public static void run() {
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
		try {
			tomcat.start();
		} catch (LifecycleException e) {
			e.printStackTrace();
		}
		tomcat.getServer().await();
	}

}
