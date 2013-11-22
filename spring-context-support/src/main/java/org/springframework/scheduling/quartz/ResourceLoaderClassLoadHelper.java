/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.scheduling.quartz;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.spi.ClassLoadHelper;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Wrapper that adapts from the Quartz {@link ClassLoadHelper} interface
 * onto Spring's {@link ResourceLoader} interface. Used by default when
 * the SchedulerFactoryBean runs in a Spring ApplicationContext.
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see SchedulerFactoryBean#setApplicationContext
 */
public class ResourceLoaderClassLoadHelper implements ClassLoadHelper {

	protected static final Log logger = LogFactory.getLog(ResourceLoaderClassLoadHelper.class);

	private ResourceLoader resourceLoader;


	/**
	 * Create a new ResourceLoaderClassLoadHelper for the default
	 * ResourceLoader.
	 * @see SchedulerFactoryBean#getConfigTimeResourceLoader()
	 */
	public ResourceLoaderClassLoadHelper() {
	}

	/**
	 * Create a new ResourceLoaderClassLoadHelper for the given ResourceLoader.
	 * @param resourceLoader the ResourceLoader to delegate to
	 */
	public ResourceLoaderClassLoadHelper(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	@Override
	public void initialize() {
		if (this.resourceLoader == null) {
			this.resourceLoader = SchedulerFactoryBean.getConfigTimeResourceLoader();
			if (this.resourceLoader == null) {
				this.resourceLoader = new DefaultResourceLoader();
			}
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class loadClass(String name) throws ClassNotFoundException {
		return this.resourceLoader.getClassLoader().loadClass(name);
	}

	@SuppressWarnings("unchecked")
	public <T> Class<? extends T> loadClass(String name, Class<T> clazz) throws ClassNotFoundException {
		return loadClass(name);
	}

	@Override
	public URL getResource(String name) {
		Resource resource = this.resourceLoader.getResource(name);
		try {
			return resource.getURL();
		}
		catch (FileNotFoundException ex) {
			return null;
		}
		catch (IOException ex) {
			logger.warn("Could not load " + resource);
			return null;
		}
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		Resource resource = this.resourceLoader.getResource(name);
		try {
			return resource.getInputStream();
		}
		catch (FileNotFoundException ex) {
			return null;
		}
		catch (IOException ex) {
			logger.warn("Could not load " + resource);
			return null;
		}
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.resourceLoader.getClassLoader();
	}

}
