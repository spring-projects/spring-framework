package com.atlwj.demo.ioc.xml.resource;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;


public class MyResource implements ResourceLoaderAware {

	private ResourceLoader resourceLoader;
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public Resource getResource(String locationPath){
		return resourceLoader.getResource(locationPath);
	}
}
