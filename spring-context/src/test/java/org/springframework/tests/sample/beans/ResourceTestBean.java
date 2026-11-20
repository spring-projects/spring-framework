/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.tests.sample.beans;

import java.io.InputStream;
import java.util.Map;

import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;

/**
 * @author Juergen Hoeller
 * @since 01.04.2004
 */
public class ResourceTestBean {

	private Resource resource;

	private ContextResource contextResource;

	private InputStream inputStream;

	private Resource[] resourceArray;

	private Map<String, Resource> resourceMap;

	private Map<String, Resource[]> resourceArrayMap;


	public ResourceTestBean() {
	}

	public ResourceTestBean(Resource resource, InputStream inputStream) {
		this.resource = resource;
		this.inputStream = inputStream;
	}


	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public ContextResource getContextResource() {
		return contextResource;
	}

	public void setContextResource(ContextResource contextResource) {
		this.contextResource = contextResource;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public Resource[] getResourceArray() {
		return resourceArray;
	}

	public void setResourceArray(Resource[] resourceArray) {
		this.resourceArray = resourceArray;
	}

	public Map<String, Resource> getResourceMap() {
		return resourceMap;
	}

	public void setResourceMap(Map<String, Resource> resourceMap) {
		this.resourceMap = resourceMap;
	}

	public Map<String, Resource[]> getResourceArrayMap() {
		return resourceArrayMap;
	}

	public void setResourceArrayMap(Map<String, Resource[]> resourceArrayMap) {
		this.resourceArrayMap = resourceArrayMap;
	}

}
