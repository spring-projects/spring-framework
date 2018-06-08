/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Standalone XML application context, taking the context definition files
 * from the class path, interpreting plain paths as class path resource names
 * that include the package path (e.g. "mypackage/myresource.txt").
 *  独立的XML文件协议的application 应用级上下文环境，通过从类路径、解析的简单路径等获取上下文的定义文件
 *  e.g. 在包的根目录存放的文件
 *
 * Useful for test harnesses as well as for application contexts embedded within JARs.
 * 对测试非常有用，同时对以jar为基础的 application contexts非常有用
 *
 *
 * 这个配置定位默认是可以重载的
 * <p>The config location defaults can be overridden via {@link #getConfigLocations},
 * Config locations can either denote concrete files like "/myfiles/context.xml"
 * or Ant-style patterns like "/myfiles/*-context.xml" (see the
 * {@link org.springframework.util.AntPathMatcher} javadoc for pattern details).
 *
 *
 * 配置文件中多个definition ，后面的将会覆盖前面的
 * <p>Note: In case of multiple config locations, later bean definitions will
 * override ones defined in earlier loaded files.
 *
 *
 * This can be leveraged to
 * deliberately override certain bean definitions via an extra XML file.
 *  这可以用来通过一个额外的XML文件故意覆盖某些bean定义
 *
 * <p><b>This is a simple, one-stop shop convenience ApplicationContext.
 * 这是一个简答的一步到位的方便的 ApplicationContext
 *
 * Consider using the {@link GenericApplicationContext} class in combination
 * with an {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}
 * for more flexible context setup.</b>
 * 可以通过 GenericApplicationContext 和 XmlBeanDefinitionReader实现更加灵活的配置
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getResource
 * @see #getResourceByPath
 * @see GenericApplicationContext
 *
 *
 *
 *
 * ------------------------------------
 * xuzqjob@163.com
 * zhiqiang.xu
 *
 * 自己的理解：
 * 一个应用对应一个ApplicationContext
 * 每个ApplicationContext读取的资源文件可以从不同的地方获取
 * 每个ApplicationContext读取的资源文件可以是不用的格式
 * 每个ApplicationContext读取的资源文件可以是以不同的方式读取的
 *
 * 所有在ApplicationContext 上可以包装很多个丰富的类型，ClassPathXmlApplicationContext只是其中一个
 *
 * ---------------------------------------------------------
 *
 */
public class ClassPathXmlApplicationContext extends AbstractXmlApplicationContext {

	@Nullable
	private Resource[] configResources;


	/**
	 * Create a new ClassPathXmlApplicationContext for bean-style configuration.
	 *
	 * 创建一个ClassPathXmlApplicationContext  使用bean-style 风格的配置
	 *
	 * @see #setConfigLocation
	 * @see #setConfigLocations
	 * @see #afterPropertiesSet()
	 */
	public ClassPathXmlApplicationContext() {
	}

	/**
	 * Create a new ClassPathXmlApplicationContext for bean-style configuration.
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
	 *
	 * 通过从传进去的 XML file中读取数据来加载 ClassPathXmlApplicationContext类
	 * 支持自动刷新 context
	 *
	 * @param configLocation resource location  xml文件
	 * @throws BeansException if context creation failed
	 */
	public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
		this(new String[] {configLocation}, true, null);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext, loading the definitions
	 * from the given XML files and automatically refreshing the context.
	 * @param configLocations array of resource locations
	 * @throws BeansException if context creation failed
	 *
	 * 和上面一个方法类似，不同的是传的参数是一个可变参数的字符串
	 */
	public ClassPathXmlApplicationContext(String... configLocations) throws BeansException {
		this(configLocations, true, null);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext with the given parent,
	 * loading the definitions from the given XML files and automatically
	 * refreshing the context.
	 *
	 * 不止一个xml文件，是一个数组，也就是应用中有多个xml文件
	 * @param configLocations array of resource locations
	 *
	 * 把父应用的上下文环境也传进去了
	 * @param parent the parent context
	 * @throws BeansException if context creation failed
	 */
	public ClassPathXmlApplicationContext(String[] configLocations, @Nullable ApplicationContext parent)
			throws BeansException {

		this(configLocations, true, parent);
	}

	/**
	 *
	 * 支持通过参数控制是否自动刷新
	 *
	 * Create a new ClassPathXmlApplicationContext, loading the definitions
	 * from the given XML files.
	 * @param configLocations array of resource locations
	 * @param refresh whether to automatically refresh the context,
	 * loading all bean definitions and creating all singletons.
	 * Alternatively, call refresh manually after further configuring the context.
	 * @throws BeansException if context creation failed
	 * @see #refresh()
	 */
	public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
		this(configLocations, refresh, null);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext with the given parent,
	 * loading the definitions from the given XML files.
	 * @param configLocations array of resource locations ///// xml配置文件数组
	 * @param refresh whether to automatically refresh the context, /////是否支持自动刷新
	 * loading all bean definitions and creating all singletons.
	 * Alternatively, call refresh manually after further configuring the context.
	 * @param parent the parent context /////父应用的上下文环境
	 * @throws BeansException if context creation failed
	 * @see #refresh()
	 */
	public ClassPathXmlApplicationContext(
			String[] configLocations, boolean refresh, @Nullable ApplicationContext parent)
			throws BeansException {

		super(parent);
		setConfigLocations(configLocations);
		if (refresh) {
			refresh();
		}
	}


	/**
	 * Create a new ClassPathXmlApplicationContext, loading the definitions
	 * from the given XML file and automatically refreshing the context.
	 *
	 * <p>This is a convenience method to load class path resources relative to a
	 * given Class. 这个方法加载给定类的类路径资源很方便
	 *
	 * For full flexibility, consider using a GenericApplicationContext
	 * with an XmlBeanDefinitionReader and a ClassPathResource argument.
	 * 为了灵活，建议用GenericApplicationContext
	 * 带有 XmlBeanDefinitionReader and a ClassPathResource argument的
	 *
	 * @param path relative (or absolute) path within the class path
	 * @param clazz the class to load resources with (basis for the given paths)
	 * @throws BeansException if context creation failed
	 * @see org.springframework.core.io.ClassPathResource#ClassPathResource(String, Class)
	 * @see org.springframework.context.support.GenericApplicationContext
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	public ClassPathXmlApplicationContext(String path, Class<?> clazz) throws BeansException {
		this(new String[] {path}, clazz);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext, loading the definitions
	 * from the given XML files and automatically refreshing the context.
	 *
	 *
	 * @param paths array of relative (or absolute) paths within the class path //路径数组，
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
	 * @param paths array of relative (or absolute) paths within the class path
	 * @param clazz the class to load resources with (basis for the given paths)
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
