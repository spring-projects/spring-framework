package com.lxcecho.resources.di;

import org.springframework.core.io.Resource;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class ResourceBean {

	private Resource resource;

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public Resource getResource() {
		return resource;
	}

	public void parse() {
		System.out.println(resource.getFilename());
		System.out.println(resource.getDescription());
	}
}
