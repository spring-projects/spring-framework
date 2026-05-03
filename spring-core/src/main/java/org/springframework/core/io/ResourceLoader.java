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

package org.springframework.core.io;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ResourceUtils;

/**
 * Strategy interface for loading resources (for example, class path or file system
 * resources). An {@link org.springframework.context.ApplicationContext}
 * is required to provide this functionality plus extended
 * {@link org.springframework.core.io.support.ResourcePatternResolver} support.
 *
 * <p>{@link DefaultResourceLoader} is a standalone implementation that is
 * usable outside an ApplicationContext and is also used by {@link ResourceEditor}.
 *
 * <p>Bean properties of type {@code Resource} and {@code Resource[]} can be populated
 * from Strings when running in an ApplicationContext, using the particular
 * context's resource loading strategy.
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see Resource
 * @see org.springframework.core.io.support.ResourcePatternResolver
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 */
public interface ResourceLoader {

	/**
	 * Pseudo URL prefix for loading from the class path: {@value}.
	 * <p>This retrieves the "nearest" matching resource in the classpath.
	 * @see ClassLoader#getResource
	 */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;

	/**
	 * Pseudo URL prefix for all matching resources from the class path: {@value}.
	 * <p>This differs from the common {@link #CLASSPATH_URL_PREFIX "classpath:"} prefix
	 * in that it retrieves all matching resources for a given path. For example, to
	 * locate all "messages.properties" files in the root of all deployed JAR files
	 * you can use the location pattern {@code "classpath*:/messages.properties"}.
	 * <p>As of Spring Framework 6.0, the semantics for the {@code "classpath*:"}
	 * prefix have been expanded to include the module path as well as the class path.
	 * <p>As of Spring Framework 7.1, this prefix is supported for {@link #getResource}
	 * calls as well (exposing a multi-content resource handle), rather than just for
	 * {@link org.springframework.core.io.support.ResourcePatternResolver#getResources}.
	 * @since 7.1 (previously only declared on the
	 * {@link org.springframework.core.io.support.ResourcePatternResolver} sub-interface)
	 * @see ClassLoader#getResources
	 * @see Resource#consumeContent
	 */
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:";


	/**
	 * Return a {@code Resource} handle for the specified resource location.
	 * <p>The handle should always be a reusable resource descriptor,
	 * allowing for multiple {@link Resource#getInputStream()} calls.
	 * <ul>
	 * <li>Must support fully qualified URLs, for example, "file:C:/test.properties".
	 * <li>Must support classpath pseudo-URLs, for example, "classpath:test.properties".
	 * (Exposing the "nearest" resource in the classpath; see {@link ClassLoader#getResource}.)
	 * <li>Should support classpath-all URLs, for example, "classpath*:test.properties".
	 * (If supported, the returned {@code Resource} needs to expose the entire content of
	 * all same-named resources in the classpath through {@link Resource#consumeContent};
	 * see {@link ClassLoader#getResources}.
	 * For individual access to each such matching resource in the classpath, use
	 * {@link org.springframework.core.io.support.ResourcePatternResolver#getResources}.)
	 * <li>Should support relative file paths, for example, "WEB-INF/test.properties".
	 * (This will be implementation-specific, typically provided by an
	 * ApplicationContext implementation.)
	 * </ul>
	 * <p>Note that a {@code Resource} handle does not imply an existing resource;
	 * you need to invoke {@link Resource#exists} to check for existence.
	 * @param location the resource location
	 * @return a corresponding {@code Resource} handle (never {@code null})
	 * @see #CLASSPATH_URL_PREFIX
	 * @see #CLASSPATH_ALL_URL_PREFIX
	 * @see Resource#exists()
	 * @see Resource#consumeContent
	 */
	Resource getResource(String location);

	/**
	 * Expose the {@link ClassLoader} used by this {@code ResourceLoader}.
	 * <p>Clients which need to access the {@code ClassLoader} directly can do so
	 * in a uniform manner with the {@code ResourceLoader}, rather than relying
	 * on the thread context {@code ClassLoader}.
	 * @return the {@code ClassLoader}
	 * (only {@code null} if even the system {@code ClassLoader} isn't accessible)
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable ClassLoader getClassLoader();

}
