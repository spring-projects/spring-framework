/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Standalone XML application context, taking the context definition files
 * from the class path, interpreting plain paths as class path resource names
 * that include the package path (e.g. "mypackage/myresource.txt"). Useful for
 * test harnesses as well as for application contexts embedded within JARs.
 *
 * <p>The config location defaults can be overridden via {@link #getConfigLocations},
 * Config locations can either denote concrete files like "/myfiles/context.xml"
 * or Ant-style patterns like "/myfiles/*-context.xml" (see the
 * {@link org.springframework.util.AntPathMatcher} javadoc for pattern details).
 *
 * <p>Note: In case of multiple config locations, later bean definitions will
 * override ones defined in earlier loaded files. This can be leveraged to
 * deliberately override certain bean definitions via an extra XML file.
 *
 * <p><b>This is a simple, one-stop shop convenience ApplicationContext.
 * Consider using the {@link GenericApplicationContext} class in combination
 * with an {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}
 * for more flexible context setup.</b>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getResource
 * @see #getResourceByPath
 * @see GenericApplicationContext
 */
public class ClassPathXmlApplicationContext extends AbstractXmlApplicationContext {

	@Nullable
	private Resource[] configResources;


	/**
	 * Create a new ClassPathXmlApplicationContext for bean-style configuration.
	 * 为bean样式的配置创建一个新的ClassPathXmlApplicationContext
	 *
	 * @see #setConfigLocation
	 * @see #setConfigLocations
	 * @see #afterPropertiesSet()
	 */
	public ClassPathXmlApplicationContext() {
	}

	/**
	 * Create a new ClassPathXmlApplicationContext for bean-style configuration.
	 * 为bean样式的配置创建一个新的ClassPathXmlApplicationContext
	 *
	 * @param parent the parent context
	 * @see #setConfigLocation
	 * @see #setConfigLocations
	 * @see #afterPropertiesSet()
	 */
	public ClassPathXmlApplicationContext(ApplicationContext parent) {
		super(parent);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext, loading the definitions
	 * from the given XML file and automatically refreshing the context.
	 * 创建一个新的ClassPathXmlApplicationContext, 从给定的XML文件加载定义并自动刷新上下文
	 *
	 * @param configLocation resource location
	 * @throws BeansException if context creation failed
	 */
	public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
		this(new String[]{configLocation}, true, null);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext, loading the definitions
	 * from the given XML files and automatically refreshing the context.
	 * 创建一个新的ClassPathXmlApplicationContext, 从给定的XML文件加载定义并自动刷新上下文
	 *
	 * @param configLocations array of resource locations
	 * @throws BeansException if context creation failed
	 */
	public ClassPathXmlApplicationContext(String... configLocations) throws BeansException {
		this(configLocations, true, null);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext with the given parent,
	 * loading the definitions from the given XML files and automatically
	 * refreshing the context.
	 * 使用给定的父级创建一个新的ClassPathXmlApplicationContext, 从给定的XML文件加载定义并自动刷新上下文
	 *
	 * @param configLocations array of resource locations
	 * @param parent          the parent context
	 * @throws BeansException if context creation failed
	 */
	public ClassPathXmlApplicationContext(String[] configLocations, @Nullable ApplicationContext parent)
			throws BeansException {

		this(configLocations, true, parent);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext, loading the definitions
	 * from the given XML files.
	 * 创建一个新的ClassPathXmlApplicationContext, 从给定的XML文件加载定义
	 *
	 * @param configLocations array of resource locations
	 * @param refresh         whether to automatically refresh the context,
	 *                        loading all bean definitions and creating all singletons.
	 *                        Alternatively, call refresh manually after further configuring the context.
	 * @throws BeansException if context creation failed
	 * @see #refresh()
	 */
	public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
		this(configLocations, refresh, null);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext with the given parent,
	 * loading the definitions from the given XML files.
	 * 使用给定的父级创建新的ClassPathXmlApplicationContext, 从给定的XML文件加载定义
	 *
	 * @param configLocations array of resource locations
	 * @param refresh         whether to automatically refresh the context,
	 *                        loading all bean definitions and creating all singletons.
	 *                        Alternatively, call refresh manually after further configuring the context.
	 * @param parent          the parent context
	 * @throws BeansException if context creation failed
	 * @see #refresh()
	 */
	public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh, @Nullable ApplicationContext parent) throws BeansException {

		super(parent);
		setConfigLocations(configLocations);
		if (refresh) {
			refresh();
		}
	}


	/**
	 * Create a new ClassPathXmlApplicationContext, loading the definitions
	 * from the given XML file and automatically refreshing the context.
	 * <p>This is a convenience method to load class path resources relative to a
	 * given Class. For full flexibility, consider using a GenericApplicationContext
	 * with an XmlBeanDefinitionReader and a ClassPathResource argument.
	 * 创建一个新的ClassPathXmlApplicationContext, 从给定的XML文件加载定义并自动刷新上下文
	 * 这是一种相对于给定类加载类路径资源的方便方法。为了获得充分的灵活性, 可以考虑将GenericApplicationContext与XmlBeanDefinitionReader和ClassPathResource参数一起使用
	 *
	 * @param path  relative (or absolute) path within the class path
	 * @param clazz the class to load resources with (basis for the given paths)
	 * @throws BeansException if context creation failed
	 * @see org.springframework.core.io.ClassPathResource#ClassPathResource(String, Class)
	 * @see org.springframework.context.support.GenericApplicationContext
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	public ClassPathXmlApplicationContext(String path, Class<?> clazz) throws BeansException {
		this(new String[]{path}, clazz);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext, loading the definitions
	 * from the given XML files and automatically refreshing the context.
	 * 创建一个新的ClassPathXmlApplicationContext, 从给定的XML文件加载定义并自动刷新上下文
	 *
	 * @param paths array of relative (or absolute) paths within the class path
	 * @param clazz the class to load resources with (basis for the given paths)
	 * @throws BeansException if context creation failed
	 * @see org.springframework.core.io.ClassPathResource#ClassPathResource(String, Class)
	 * @see org.springframework.context.support.GenericApplicationContext
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	public ClassPathXmlApplicationContext(String[] paths, Class<?> clazz) throws BeansException {
		this(paths, clazz, null);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext with the given parent,
	 * loading the definitions from the given XML files and automatically
	 * refreshing the context.
	 * 使用给定的父级创建一个新的ClassPathXmlApplicationContext, 从给定的XML文件加载定义并自动刷新上下文
	 *
	 * @param paths  array of relative (or absolute) paths within the class path
	 * @param clazz  the class to load resources with (basis for the given paths)
	 * @param parent the parent context
	 * @throws BeansException if context creation failed
	 * @see org.springframework.core.io.ClassPathResource#ClassPathResource(String, Class)
	 * @see org.springframework.context.support.GenericApplicationContext
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	public ClassPathXmlApplicationContext(String[] paths, Class<?> clazz, @Nullable ApplicationContext parent)
			throws BeansException {

		super(parent);
		Assert.notNull(paths, "Path array must not be null");
		Assert.notNull(clazz, "Class argument must not be null");
		this.configResources = new Resource[paths.length];
		for (int i = 0; i < paths.length; i++) {
			this.configResources[i] = new ClassPathResource(paths[i], clazz);
		}
		refresh();
	}


	@Override
	@Nullable
	protected Resource[] getConfigResources() {
		return this.configResources;
	}

}
