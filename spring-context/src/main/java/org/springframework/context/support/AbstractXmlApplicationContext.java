/*
 * Copyright 2002-2023 the original author or authors.
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
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.BeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import java.io.IOException;

/**
 * Convenient base class for {@link org.springframework.context.ApplicationContext}
 * implementations, drawing configuration from XML documents containing bean definitions
 * understood by an {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}.
 *
 * <p>Subclasses just have to implement the {@link #getConfigResources} and/or
 * the {@link #getConfigLocations} method. Furthermore, they might override
 * the {@link #getResourceByPath} hook to interpret relative paths in an
 * environment-specific fashion, and/or {@link #getResourcePatternResolver}
 * for extended pattern resolution.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getConfigResources
 * @see #getConfigLocations
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 */
public abstract class AbstractXmlApplicationContext extends AbstractRefreshableConfigApplicationContext {

	private boolean validating = true;


	/**
	 * Create a new AbstractXmlApplicationContext with no parent.
	 * 创建不带父级的新 AbstractXmlApplicationContext
	 */
	public AbstractXmlApplicationContext() {
	}

	/**
	 * Create a new AbstractXmlApplicationContext with the given parent context.
	 * 使用给定的父上下文创建新的 AbstractXmlApplicationContext
	 * @param parent the parent context
	 */
	public AbstractXmlApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * Set whether to use XML validation. Default is {@code true}.
	 */
	public void setValidating(boolean validating) {
		this.validating = validating;
	}


	/**
	 * Loads the bean definitions via an XmlBeanDefinitionReader.
	 * 通过XmlBeanDefinitionReader加载bean定义
	 *
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 * @see #initBeanDefinitionReader
	 * @see #loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {

		// Create a new XmlBeanDefinitionReader for the given BeanFactory.
		// 为指定 BeanFactory 创建 XmlBeanDefinitionReader
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		// Configure the bean definition reader with this context's
		// resource loading environment.
		// 对 beanDefinitionReader 进行配置，包括环境、资源加载器、实体解析器
		beanDefinitionReader.setEnvironment(getEnvironment());
		beanDefinitionReader.setResourceLoader(this);
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		// Allow a subclass to provide custom initialization of the reader,
		// then proceed with actually loading the bean definitions.
		// 对 BeanDefinitionReader 进行设置, 可以覆盖
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
	}

	/**
	 * Initialize the bean definition reader used for loading the bean definitions
	 * of this context. The default implementation sets the validating flag.
	 * <p>Can be overridden in subclasses, e.g. for turning off XML validation
	 * or using a different {@link BeanDefinitionDocumentReader} implementation.
	 * 初始化用于加载此上下文的bean定义的bean定义读取器。默认实现设置验证标志
	 * 可以在子类中重写，例如用于关闭XML验证或使用不同的{@link BeanDefinitionDocumentReader} 实现。
	 *
	 * @param reader the bean definition reader used by this context
	 * @see XmlBeanDefinitionReader#setValidating
	 * @see XmlBeanDefinitionReader#setDocumentReaderClass
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
		reader.setValidating(this.validating);
	}

	/**
	 * Load the bean definitions with the given XmlBeanDefinitionReader.
	 * <p>The lifecycle of the bean factory is handled by the {@link #refreshBeanFactory}
	 * method; hence this method is just supposed to load and/or register bean definitions.
	 * 使用给定的XmlBeanDefinitionReader加载bean定义
	 * bean工厂的生命周期由{@link #refreshBeanFactory}方法处理；因此，该方法只用于加载和或注册bean定义
	 *
	 * @param reader the XmlBeanDefinitionReader to use
	 * @throws BeansException in case of bean registration errors
	 * @throws IOException    if the required XML document isn't found
	 * @see #refreshBeanFactory
	 * @see #getConfigLocations
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
		Resource[] configResources = getConfigResources();
		if (configResources != null) {
			reader.loadBeanDefinitions(configResources);
		}
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			reader.loadBeanDefinitions(configLocations);
		}
	}

	/**
	 * Return an array of Resource objects, referring to the XML bean definition
	 * files that this context should be built with.
	 * <p>The default implementation returns {@code null}. Subclasses can override
	 * this to provide pre-built Resource objects rather than location Strings.
	 * 返回一个Resource对象数组，引用应该用来构建此上下文的XML bean定义文件
	 * 默认实现返回 {@code null}。子类可以覆盖此项以提供预构建的资源对象，而不是位置字符串
	 *
	 * @return an array of Resource objects, or {@code null} if none
	 * @see #getConfigLocations()
	 */
	@Nullable
	protected Resource[] getConfigResources() {
		return null;
	}

}
