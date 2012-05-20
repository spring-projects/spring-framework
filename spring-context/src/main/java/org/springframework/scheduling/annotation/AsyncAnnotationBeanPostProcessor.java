/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.scheduling.annotation;

import java.lang.annotation.Annotation;
import java.util.concurrent.Executor;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Bean post-processor that automatically applies asynchronous invocation
 * behavior to any bean that carries the {@link Async} annotation at class or
 * method-level by adding a corresponding {@link AsyncAnnotationAdvisor} to the
 * exposed proxy (either an existing AOP proxy or a newly generated proxy that
 * implements all of the target's interfaces).
 *
 * <p>The {@link TaskExecutor} responsible for the asynchronous execution may
 * be provided as well as the annotation type that indicates a method should be
 * invoked asynchronously. If no annotation type is specified, this post-
 * processor will detect both Spring's {@link Async @Async} annotation as well
 * as the EJB 3.1 <code>javax.ejb.Asynchronous</code> annotation.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 * @see Async
 * @see AsyncAnnotationAdvisor
 */
@SuppressWarnings("serial")
public class AsyncAnnotationBeanPostProcessor extends ProxyConfig
		implements BeanPostProcessor, BeanClassLoaderAware, BeanFactoryAware,
		InitializingBean, Ordered {

	private Class<? extends Annotation> asyncAnnotationType;

	private Executor executor;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private AsyncAnnotationAdvisor asyncAnnotationAdvisor;

	/**
	 * This should run after all other post-processors, so that it can just add
	 * an advisor to existing proxies rather than double-proxy.
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;

	private BeanFactory beanFactory;


	/**
	 * Set the 'async' annotation type to be detected at either class or method
	 * level. By default, both the {@link Async} annotation and the EJB 3.1
	 * <code>javax.ejb.Asynchronous</code> annotation will be detected.
	 * <p>This setter property exists so that developers can provide their own
	 * (non-Spring-specific) annotation type to indicate that a method (or all
	 * methods of a given class) should be invoked asynchronously.
	 * @param asyncAnnotationType the desired annotation type
	 */
	public void setAsyncAnnotationType(Class<? extends Annotation> asyncAnnotationType) {
		Assert.notNull(asyncAnnotationType, "'asyncAnnotationType' must not be null");
		this.asyncAnnotationType = asyncAnnotationType;
	}

	/**
	 * Set the {@link Executor} to use when invoking methods asynchronously.
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public void afterPropertiesSet() {
		this.asyncAnnotationAdvisor = (this.executor != null ?
				new AsyncAnnotationAdvisor(this.executor) : new AsyncAnnotationAdvisor());
		if (this.asyncAnnotationType != null) {
			this.asyncAnnotationAdvisor.setAsyncAnnotationType(this.asyncAnnotationType);
		}
		this.asyncAnnotationAdvisor.setBeanFactory(this.beanFactory);
	}

	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}


	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (bean instanceof AopInfrastructureBean) {
			// Ignore AOP infrastructure such as scoped proxies.
			return bean;
		}
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		if (AopUtils.canApply(this.asyncAnnotationAdvisor, targetClass)) {
			if (bean instanceof Advised) {
				((Advised) bean).addAdvisor(0, this.asyncAnnotationAdvisor);
				return bean;
			}
			else {
				ProxyFactory proxyFactory = new ProxyFactory(bean);
				// Copy our properties (proxyTargetClass etc) inherited from ProxyConfig.
				proxyFactory.copyFrom(this);
				proxyFactory.addAdvisor(this.asyncAnnotationAdvisor);
				return proxyFactory.getProxy(this.beanClassLoader);
			}
		}
		else {
			// No async proxy needed.
			return bean;
		}
	}

}
