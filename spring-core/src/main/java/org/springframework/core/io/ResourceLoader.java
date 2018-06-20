/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * Strategy interface for loading resources (e.. class path or file system
 * resources).
 *
 *                        ***********从文件系统或则从类路径加载资源的策略接口**********
 *
 *                        ********************顶级接口****************
 *
 *
 * An {@link org.springframework.context.ApplicationContext}
 * is required to provide this functionality, plus extended
 * {@link org.springframework.core.io.support.ResourcePatternResolver} support.
 *
 * ApplicationContext类必须提供加载资源的功能，同时还有它自己的扩展方法，
 * ResourcePatternResolver支持这个功能
 *
 * <p>{@link DefaultResourceLoader} is a standalone implementation that is
 * usable outside an ApplicationContext, also used by {@link ResourceEditor}.
 * DefaultResourceLoader提供一个默认的，单独的加载类资源的接口，
 * 它除了用在ApplicationContext中还可以用在ResourceEditor中
 *
 * <p>Bean properties of type Resource and Resource array can be populated
 * from Strings when running in an ApplicationContext, using the particular
 * context's resource loading strategy.
 *
 * Bean属性或者资源类型可以发布成字符串
 *
 *
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see Resource
 * @see org.springframework.core.io.support.ResourcePatternResolver
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 */
public interface ResourceLoader {

	/** Pseudo URL prefix for loading from the class path: "classpath:" */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;


	/**
	 * Return a Resource handle for the specified resource location.
	 * 通过给定的资源的位置返回一个资源解析器
	 *
	 *
	 * <p>The handle should always be a reusable resource descriptor,
	 * allowing for multiple {@link Resource#getInputStream()} calls.
	 * <p><ul>
	 *
	 *  这个资源解析器必须是可以被重复使用的，重复调用
	 *
	 * <li>Must support fully qualified URLs, e.g. "file:C:/test.dat".    必须支持绝对路径
	 * <li>Must support classpath pseudo-URLs, e.g. "classpath:test.dat". 必须支持类路径
	 * <li>Should support relative file paths, e.g. "WEB-INF/test.dat".   必须支持相对路径
	 * (This will be implementation-specific, typically provided by an
	 * ApplicationContext implementation.)   典型实现者是 ApplicationContext
	 * </ul>
	 * <p>Note that a Resource handle does not imply an existing resource;
	 * you need to invoke {@link Resource#exists} to check for existence.
	 *
	 * 一个资源解析器并不意味着资源的已经存在，你需要通过检查来判断资源解析器的存在
	 *
	 *
	 * @param location the resource location
	 * @return a corresponding Resource handle (never {@code null})
	 * @see #CLASSPATH_URL_PREFIX
	 * @see Resource#exists()
	 * @see Resource#getInputStream()
	 */
	Resource getResource(String location);

	/**
	 * Expose the ClassLoader used by this ResourceLoader.
	 *
	 * 暴露这个资源解析器的类加载器
	 *
	 * <p>Clients which need to access the ClassLoader directly can do so
	 * in a uniform manner with the ResourceLoader, rather than relying
	 * on the thread context ClassLoader.
	 *
	 * 客户端可以直接获取资源解析器的类加载器，而不是依赖线程来获取类加载器，？？？？这样做有什么好处？？？
	 *
	 *
	 * @return the ClassLoader
	 * (only {@code null} if even the system ClassLoader isn't accessible)
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getClassLoader();

}
