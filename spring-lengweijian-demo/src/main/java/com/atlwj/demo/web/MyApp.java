package com.atlwj.demo.web;

import com.atlwj.demo.web.config.server.ContainerFactory;
import com.atlwj.demo.web.config.server.WebContainer;


public class MyApp {

	public static void main(String[] args) {
		run("tomcat",8081,"/lengwj-web");
	}

	private static void run(String containerName, Integer port, String contextPath) {
		WebContainer container = ContainerFactory.getContainer(containerName,port,contextPath);
		container.run();
	}

}
