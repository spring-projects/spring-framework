/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.ui.velocity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

import org.springframework.util.StringUtils;

/**
 * Velocity ResourceLoader adapter that loads via a Spring ResourceLoader.
 * Used by VelocityEngineFactory for any resource loader path that cannot
 * be resolved to a <code>java.io.File</code>.
 *
 * <p>Note that this loader does not allow for modification detection:
 * Use Velocity's default FileResourceLoader for <code>java.io.File</code>
 * resources.
 *
 * <p>Expects "spring.resource.loader" and "spring.resource.loader.path"
 * application attributes in the Velocity runtime: the former of type
 * <code>org.springframework.core.io.ResourceLoader</code>, the latter a String.
 *
 * @author Juergen Hoeller
 * @since 14.03.2004
 * @see VelocityEngineFactory#setResourceLoaderPath
 * @see org.springframework.core.io.ResourceLoader
 * @see org.apache.velocity.runtime.resource.loader.FileResourceLoader
 */
public class SpringResourceLoader extends ResourceLoader {

	public static final String NAME = "spring";

	public static final String SPRING_RESOURCE_LOADER_CLASS = "spring.resource.loader.class";

	public static final String SPRING_RESOURCE_LOADER_CACHE = "spring.resource.loader.cache";

	public static final String SPRING_RESOURCE_LOADER = "spring.resource.loader";

	public static final String SPRING_RESOURCE_LOADER_PATH = "spring.resource.loader.path";


	protected final Log logger = LogFactory.getLog(getClass());

	private org.springframework.core.io.ResourceLoader resourceLoader;

	private String[] resourceLoaderPaths;


	@Override
	public void init(ExtendedProperties configuration) {
		this.resourceLoader = (org.springframework.core.io.ResourceLoader)
				this.rsvc.getApplicationAttribute(SPRING_RESOURCE_LOADER);
		String resourceLoaderPath = (String) this.rsvc.getApplicationAttribute(SPRING_RESOURCE_LOADER_PATH);
		if (this.resourceLoader == null) {
			throw new IllegalArgumentException(
					"'resourceLoader' application attribute must be present for SpringResourceLoader");
		}
		if (resourceLoaderPath == null) {
			throw new IllegalArgumentException(
					"'resourceLoaderPath' application attribute must be present for SpringResourceLoader");
		}
		this.resourceLoaderPaths = StringUtils.commaDelimitedListToStringArray(resourceLoaderPath);
		for (int i = 0; i < this.resourceLoaderPaths.length; i++) {
			String path = this.resourceLoaderPaths[i];
			if (!path.endsWith("/")) {
				this.resourceLoaderPaths[i] = path + "/";
			}
		}
		if (logger.isInfoEnabled()) {
			logger.info("SpringResourceLoader for Velocity: using resource loader [" + this.resourceLoader +
					"] and resource loader paths " + Arrays.asList(this.resourceLoaderPaths));
		}
	}

	@Override
	public InputStream getResourceStream(String source) throws ResourceNotFoundException {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for Velocity resource with name [" + source + "]");
		}
		for (int i = 0; i < this.resourceLoaderPaths.length; i++) {
			org.springframework.core.io.Resource resource =
					this.resourceLoader.getResource(this.resourceLoaderPaths[i] + source);
			try {
				return resource.getInputStream();
			}
			catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not find Velocity resource: " + resource);
				}
			}
		}
		throw new ResourceNotFoundException(
				"Could not find resource [" + source + "] in Spring resource loader path");
	}

	@Override
	public boolean isSourceModified(Resource resource) {
		return false;
	}

	@Override
	public long getLastModified(Resource resource) {
		return 0;
	}

}
