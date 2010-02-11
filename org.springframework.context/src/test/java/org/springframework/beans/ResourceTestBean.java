/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

import java.io.InputStream;
import java.util.Map;

import org.springframework.core.io.Resource;

/**
 * @author Juergen Hoeller
 * @since 01.04.2004
 */
public class ResourceTestBean {

	private Resource resource;

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
