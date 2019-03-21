/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.instrument.classloading;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Subclass of ShadowingClassLoader that overrides attempts to
 * locate certain files.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0
 */
public class ResourceOverridingShadowingClassLoader extends ShadowingClassLoader {

	private static final Enumeration<URL> EMPTY_URL_ENUMERATION = new Enumeration<URL>() {
		@Override
		public boolean hasMoreElements() {
			return false;
		}
		@Override
		public URL nextElement() {
			throw new UnsupportedOperationException("Should not be called. I am empty.");
		}
	};


	/**
	 * Key is asked for value: value is actual value
	 */
	private Map<String, String> overrides = new HashMap<String, String>();


	/**
	 * Create a new ResourceOverridingShadowingClassLoader,
	 * decorating the given ClassLoader.
	 * @param enclosingClassLoader the ClassLoader to decorate
	 */
	public ResourceOverridingShadowingClassLoader(ClassLoader enclosingClassLoader) {
		super(enclosingClassLoader);
	}


	/**
	 * Return the resource (if any) at the new path
	 * on an attempt to locate a resource at the old path.
	 * @param oldPath the path requested
	 * @param newPath the actual path to be looked up
	 */
	public void override(String oldPath, String newPath) {
		this.overrides.put(oldPath, newPath);
	}

	/**
	 * Ensure that a resource with the given path is not found.
	 * @param oldPath the path of the resource to hide even if
	 * it exists in the parent ClassLoader
	 */
	public void suppress(String oldPath) {
		this.overrides.put(oldPath, null);
	}

	/**
	 * Copy all overrides from the given ClassLoader.
	 * @param other the other ClassLoader to copy from
	 */
	public void copyOverrides(ResourceOverridingShadowingClassLoader other) {
		Assert.notNull(other, "Other ClassLoader must not be null");
		this.overrides.putAll(other.overrides);
	}


	@Override
	public URL getResource(String requestedPath) {
		if (this.overrides.containsKey(requestedPath)) {
			String overriddenPath = this.overrides.get(requestedPath);
			return (overriddenPath != null ? super.getResource(overriddenPath) : null);
		}
		else {
			return super.getResource(requestedPath);
		}
	}

	@Override
	public InputStream getResourceAsStream(String requestedPath) {
		if (this.overrides.containsKey(requestedPath)) {
			String overriddenPath = this.overrides.get(requestedPath);
			return (overriddenPath != null ? super.getResourceAsStream(overriddenPath) : null);
		}
		else {
			return super.getResourceAsStream(requestedPath);
		}
	}

	@Override
	public Enumeration<URL> getResources(String requestedPath) throws IOException {
		if (this.overrides.containsKey(requestedPath)) {
			String overriddenLocation = this.overrides.get(requestedPath);
			return (overriddenLocation != null ?
					super.getResources(overriddenLocation) : EMPTY_URL_ENUMERATION);
		}
		else {
			return super.getResources(requestedPath);
		}
	}

}
