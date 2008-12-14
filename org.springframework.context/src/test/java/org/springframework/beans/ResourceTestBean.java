package org.springframework.beans;

import java.io.InputStream;

import org.springframework.core.io.Resource;

/**
 * @author Juergen Hoeller
 * @since 01.04.2004
 */
public class ResourceTestBean {

	private Resource resource;

	private InputStream inputStream;

	public ResourceTestBean() {
	}

	public ResourceTestBean(Resource resource, InputStream inputStream) {
		this.resource = resource;
		this.inputStream = inputStream;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public Resource getResource() {
		return resource;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

}
