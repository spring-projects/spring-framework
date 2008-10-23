/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.context.support;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * FactoryBean that creates a Map with String keys and Resource values from
 * properties, interpreting passed-in String values as resource locations.
 *
 * <p>Extends PropertiesFactoryBean to inherit the capability of defining
 * local properties and loading from properties files.
 *
 * <p>Implements the ResourceLoaderAware interface to automatically use
 * the context ResourceLoader if running in an ApplicationContext.
 * Uses DefaultResourceLoader else.
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @since 1.0.2
 * @see org.springframework.core.io.DefaultResourceLoader
 */
public class ResourceMapFactoryBean extends PropertiesFactoryBean implements ResourceLoaderAware {

	private String resourceBasePath = "";

	private ResourceLoader resourceLoader = new DefaultResourceLoader();


	/**
	 * Set a base path to prepend to each resource location value
	 * in the properties file.
	 * <p>E.g.: resourceBasePath="/images", value="/test.gif"
	 * -> location="/images/test.gif"
	 */
	public void setResourceBasePath(String resourceBasePath) {
		this.resourceBasePath = (resourceBasePath != null ? resourceBasePath : "");
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
	}


	public Class getObjectType() {
		return Map.class;
	}

	/**
	 * Create the Map instance, populated with keys and Resource values.
	 */
	protected Object createInstance() throws IOException {
		Map resourceMap = new HashMap();
		Properties props = mergeProperties();
		for (Enumeration en = props.propertyNames(); en.hasMoreElements();) {
			String key = (String) en.nextElement();
			String location = props.getProperty(key);
			resourceMap.put(key, getResource(location));
		}
		return resourceMap;
	}

	/**
	 * Fetch the Resource handle for the given location,
	 * prepeding the resource base path.
	 * @param location the resource location
	 * @return the Resource handle
	 * @see org.springframework.core.io.ResourceLoader#getResource(String)
	 */
	protected Resource getResource(String location) {
		return this.resourceLoader.getResource(this.resourceBasePath + location);
	}

}
