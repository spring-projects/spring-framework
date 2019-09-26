package com.atlwj.demo.web.config.server;


/**
 * @author lengweijian
 */
public class ContainerFactory {

	public static WebContainer getContainer(String containerName,Integer port,String contextPath){
		switch (containerName){
			case Constants.TOMCAT_CONTAINER:
				return new TomcatContainer(port,contextPath);
			default:
				return null;
		}

	}
}
