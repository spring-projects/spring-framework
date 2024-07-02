/*
 * Copyright 2002-2020 the original author or authors.
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
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;

import java.io.IOException;

/**
 * Base class for {@link org.springframework.context.ApplicationContext}
 * implementations which are supposed to support multiple calls to {@link #refresh()},
 * creating a new internal bean factory instance every time.
 * Typically (but not necessarily), such a context will be driven by
 * a set of config locations to load bean definitions from.
 *
 * <p>The only method to be implemented by subclasses is {@link #loadBeanDefinitions},
 * which gets invoked on each refresh. A concrete implementation is supposed to load
 * bean definitions into the given
 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory},
 * typically delegating to one or more specific bean definition readers.
 *
 * <p><b>Note that there is a similar base class for WebApplicationContexts.</b>
 * {@link org.springframework.web.context.support.AbstractRefreshableWebApplicationContext}
 * provides the same subclassing strategy, but additionally pre-implements
 * all context functionality for web environments. There is also a
 * pre-defined way to receive config locations for a web context.
 *
 * <p>Concrete standalone subclasses of this base class, reading in a
 * specific bean definition format, are {@link ClassPathXmlApplicationContext}
 * and {@link FileSystemXmlApplicationContext}, which both derive from the
 * common {@link AbstractXmlApplicationContext} base class;
 * {@link org.springframework.context.annotation.AnnotationConfigApplicationContext}
 * supports {@code @Configuration}-annotated classes as a source of bean definitions.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see #loadBeanDefinitions
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.web.context.support.AbstractRefreshableWebApplicationContext
 * @see AbstractXmlApplicationContext
 * @see ClassPathXmlApplicationContext
 * @see FileSystemXmlApplicationContext
 * @see org.springframework.context.annotation.AnnotationConfigApplicationContext
 * @since 1.1.3
 */
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

	/**
	 * 是否允许Bean定义重写
	 */
	@Nullable
	private Boolean allowBeanDefinitionOverriding;

	/**
	 * 是否允许循环引用
	 */
	@Nullable
	private Boolean allowCircularReferences;

	/**
	 * Bean factory for this context.
	 * 此上下文的 Bean工厂
	 */
	@Nullable
	private volatile DefaultListableBeanFactory beanFactory;


	/**
	 * Create a new AbstractRefreshableApplicationContext with no parent.
	 * 创建不带父级的新 AbstractRefreshableApplicationContext
	 */
	public AbstractRefreshableApplicationContext() {
	}

	/**
	 * Create a new AbstractRefreshableApplicationContext with the given parent context.
	 * 使用给定的父上下文创建新的 AbstractRefreshableApplicationContext
	 *
	 * @param parent the parent context
	 */
	public AbstractRefreshableApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * Set whether it should be allowed to override bean definitions by registering
	 * a different definition with the same name, automatically replacing the former.
	 * If not, an exception will be thrown. Default is "true".
	 * 设置是否应允许通过注册具有相同名称的不同定义来覆盖bean定义, 并自动替换前者
	 * 否则, 将引发异常
	 * 默认值为“true”
	 *
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * Set whether to allow circular references between beans - and automatically
	 * try to resolve them.
	 * <p>Default is "true". Turn this off to throw an exception when encountering
	 * a circular reference, disallowing them completely.
	 * 设置是否允许bean之间的循环引用, 并自动尝试解析它们
	 * 默认值为“true”
	 * 关闭此选项可在遇到循环引用时引发异常, 从而完全禁止循环引用
	 *
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 */
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.allowCircularReferences = allowCircularReferences;
	}


	/**
	 * This implementation performs an actual refresh of this context's underlying
	 * bean factory, shutting down the previous bean factory (if any) and
	 * initializing a fresh bean factory for the next phase of the context's lifecycle.
	 * 这个实现实际刷新这个上下文的底层bean工厂，关闭以前的bean工厂（如果有的话）
	 * 并为上下文生命周期的下一阶段初始化一个新的bean工厂
	 */
	@Override
	protected final void refreshBeanFactory() throws BeansException {
		// 存在beanFactory, 则销毁beanFactory和bean
		if (hasBeanFactory()) {
			// 销毁单例bean
			destroyBeans();
			// 销毁beanFactory
			closeBeanFactory();
		}
		try {
			// 创建 DefaultListableBeanFactory
			DefaultListableBeanFactory beanFactory = createBeanFactory();
			// 为了序列化指定id, 如果需要的话, 让这个 BeanFactory 从 id 反序列化到 BeanFactory对象
			beanFactory.setSerializationId(getId());
			// 定制 BeanFactory, 设置相关属性, 包括是否允许覆盖同名称的不同定义的对象以及循环依赖以及设置@Autowired
			// 和 @Qualifier注解解释器QualifierAnnotationAutowiredElementResolver
			customizeBeanFactory(beanFactory);
			// 初始化 DocumentReader, 并进行XML文件读取及解析
			loadBeanDefinitions(beanFactory);
			// 将 BeanFactory 设置到当前实体中
			this.beanFactory = beanFactory;
		} catch (IOException ex) {
			throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
		}
	}

	@Override
	protected void cancelRefresh(BeansException ex) {
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory != null) {
			beanFactory.setSerializationId(null);
		}
		super.cancelRefresh(ex);
	}

	@Override
	protected final void closeBeanFactory() {
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory != null) {
			beanFactory.setSerializationId(null);
			this.beanFactory = null;
		}
	}

	/**
	 * Determine whether this context currently holds a bean factory,
	 * i.e. has been refreshed at least once and not been closed yet.
	 * 确定此上下文当前是否包含一个bean工厂，即至少刷新过一次，但尚未关闭
	 */
	protected final boolean hasBeanFactory() {
		return (this.beanFactory != null);
	}

	@Override
	public final ConfigurableListableBeanFactory getBeanFactory() {
		// 返回当前实体的BeanFactory属性
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory == null) {
			throw new IllegalStateException("BeanFactory not initialized or already closed - " +
					"call 'refresh' before accessing beans via the ApplicationContext");
		}
		return beanFactory;
	}

	/**
	 * Overridden to turn it into a no-op: With AbstractRefreshableApplicationContext,
	 * {@link #getBeanFactory()} serves a strong assertion for an active context anyway.
	 * 重写以将其变成无操作：使用AbstractRefreshableApplicationContext
	 * {@link #getBeanFactory()} 无论如何都为活动上下文提供一个强断言
	 */
	@Override
	protected void assertBeanFactoryActive() {
	}

	/**
	 * Create an internal bean factory for this context.
	 * Called for each {@link #refresh()} attempt.
	 * <p>The default implementation creates a
	 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory}
	 * with the {@linkplain #getInternalParentBeanFactory() internal bean factory} of this
	 * context's parent as parent bean factory. Can be overridden in subclasses,
	 * for example to customize DefaultListableBeanFactory's settings.
	 * <p>
	 * 为此上下文创建一个内部bean工厂。为每次 {@link #refresh()}尝试调用
	 * 默认实现创建一个 {@link org.springframework.beans.factory.support.DefaultListableBeanFactory}
	 * 该上下文的父级的{@linkplain #getInternalParentBeanFactory() internal bean factory}作为父级bean factory
	 * 可以在子类中重写，例如自定义DefaultListableBeanFactory的设置。
	 *
	 * @return the bean factory for this context
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowEagerClassLoading
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 */
	protected DefaultListableBeanFactory createBeanFactory() {
		return new DefaultListableBeanFactory(getInternalParentBeanFactory());
	}

	/**
	 * Customize the internal bean factory used by this context.
	 * Called for each {@link #refresh()} attempt.
	 * <p>The default implementation applies this context's
	 * {@linkplain #setAllowBeanDefinitionOverriding "allowBeanDefinitionOverriding"}
	 * and {@linkplain #setAllowCircularReferences "allowCircularReferences"} settings,
	 * if specified. Can be overridden in subclasses to customize any of
	 * {@link DefaultListableBeanFactory}'s settings.
	 * 自定义此上下文使用的内部bean工厂. 为每次{@link #refresh()} 尝试调用
	 * 默认实现应用此上下文的{@linkplain #setAllowBeanDefinitionOverriding "allowBeanDefinitionOverriding"}
	 * 和 {@linkplain #setAllowCircularReferences "allowCircularReferences"}设置（如果指定）。
	 * 可以在子类中重写以自定义｛@link DefaultListableBeanFactory｝的任何设置。
	 *
	 * @param beanFactory the newly created bean factory for this context
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see DefaultListableBeanFactory#setAllowCircularReferences
	 * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 * @see DefaultListableBeanFactory#setAllowEagerClassLoading
	 */
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
		// 如果属性 allowBeanDefinitionOverriding 不为空 , 设置给 BeanFactory 对象相应属性
		// 此属性的含义: 是否允许覆盖同名称的不同定义的对象
		if (this.allowBeanDefinitionOverriding != null) {
			beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
		}
		// 如果属性 allowCircularReferences 不为空 , 设置给 BeanFactory 对象相应属性
		// 此属性的含义: 是否允许循环依赖
		if (this.allowCircularReferences != null) {
			beanFactory.setAllowCircularReferences(this.allowCircularReferences);
		}
	}

	/**
	 * Load bean definitions into the given bean factory, typically through
	 * delegating to one or more bean definition readers.
	 * 将bean定义加载到给定的bean工厂中，通常是通过委派给一个或多个bean定义读取器
	 *
	 * @param beanFactory the bean factory to load bean definitions into
	 * @throws BeansException if parsing of the bean definitions failed
	 * @throws IOException    if loading of bean definition files failed
	 * @see org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException;

}
