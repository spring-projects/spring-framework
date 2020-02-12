/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.testfixture.index;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.index.CandidateComponentsIndexLoader;
import org.springframework.core.io.Resource;

/**
 * A test {@link ClassLoader} that can be used in a testing context to control the
 * {@code spring.components} resource that should be loaded. Can also simulate a failure
 * by throwing a configurable {@link IOException}.
 *
 * @author Stephane Nicoll
 */
public class CandidateComponentsTestClassLoader extends ClassLoader {

	/**
	 * Create a test {@link ClassLoader} that disable the use of the index, even
	 * if resources are present at the standard location.
	 * @param classLoader the classloader to use for all other operations
	 * @return a test {@link ClassLoader} that has no index
	 * @see CandidateComponentsIndexLoader#COMPONENTS_RESOURCE_LOCATION
	 */
	public static ClassLoader disableIndex(ClassLoader classLoader) {
		return new CandidateComponentsTestClassLoader(classLoader,
				Collections.enumeration(Collections.emptyList()));
	}

	/**
	 * Create a test {@link ClassLoader} that creates an index with the
	 * specified {@link Resource} instances
	 * @param classLoader the classloader to use for all other operations
	 * @return a test {@link ClassLoader} with an index built based on the
	 * specified resources.
	 */
	public static ClassLoader index(ClassLoader classLoader, Resource... resources) {
		return new CandidateComponentsTestClassLoader(classLoader,
				Collections.enumeration(Stream.of(resources).map(r -> {
					try {
						return r.getURL();
					}
					catch (Exception ex) {
						throw new IllegalArgumentException("Invalid resource " + r, ex);
					}
				}).collect(Collectors.toList())));
	}


	private final Enumeration<URL> resourceUrls;

	private final IOException cause;

	public CandidateComponentsTestClassLoader(ClassLoader classLoader, Enumeration<URL> resourceUrls) {
		super(classLoader);
		this.resourceUrls = resourceUrls;
		this.cause = null;
	}

	public CandidateComponentsTestClassLoader(ClassLoader parent, IOException cause) {
		super(parent);
		this.resourceUrls = null;
		this.cause = cause;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		if (CandidateComponentsIndexLoader.COMPONENTS_RESOURCE_LOCATION.equals(name)) {
			if (this.resourceUrls != null) {
				return this.resourceUrls;
			}
			throw this.cause;
		}
		return super.getResources(name);
	}

}
