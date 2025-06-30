/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.type.classreading;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

abstract class AbstractMetadataReaderFactory implements MetadataReaderFactory {

	private final ResourceLoader resourceLoader;


	public AbstractMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
		this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
	}

	public AbstractMetadataReaderFactory(@Nullable ClassLoader classLoader) {
		this.resourceLoader =
				(classLoader != null ? new DefaultResourceLoader(classLoader) : new DefaultResourceLoader());
	}

	public AbstractMetadataReaderFactory() {
		this.resourceLoader = new DefaultResourceLoader();
	}

	/**
	 * Return the ResourceLoader that this MetadataReaderFactory has been
	 * constructed with.
	 */
	@Override
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	@Override
	public MetadataReader getMetadataReader(String className) throws IOException {
		try {
			String resourcePath = ResourceLoader.CLASSPATH_URL_PREFIX +
					ClassUtils.convertClassNameToResourcePath(className) + ClassUtils.CLASS_FILE_SUFFIX;
			Resource resource = this.resourceLoader.getResource(resourcePath);
			return getMetadataReader(resource);
		}
		catch (FileNotFoundException ex) {
			// Maybe an inner class name using the dot name syntax? Need to use the dollar syntax here...
			// ClassUtils.forName has an equivalent check for resolution into Class references later on.
			int lastDotIndex = className.lastIndexOf('.');
			if (lastDotIndex != -1) {
				String innerClassName =
						className.substring(0, lastDotIndex) + '$' + className.substring(lastDotIndex + 1);
				String innerClassResourcePath = ResourceLoader.CLASSPATH_URL_PREFIX +
						ClassUtils.convertClassNameToResourcePath(innerClassName) + ClassUtils.CLASS_FILE_SUFFIX;
				Resource innerClassResource = this.resourceLoader.getResource(innerClassResourcePath);
				if (innerClassResource.exists()) {
					return getMetadataReader(innerClassResource);
				}
			}
			throw ex;
		}
	}

}
