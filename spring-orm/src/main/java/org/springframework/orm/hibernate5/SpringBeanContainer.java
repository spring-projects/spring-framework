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

package org.springframework.orm.hibernate5;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Spring's implementation of Hibernate 5.3's {@link BeanContainer} SPI,
 * delegating to a Spring {@link ConfigurableListableBeanFactory}.
 *
 * <p>Auto-configured by {@link LocalSessionFactoryBean#setBeanFactory},
 * programmatically supported via {@link LocalSessionFactoryBuilder#setBeanContainer},
 * and manually configurable through a "hibernate.resource.beans.container" entry
 * in JPA properties, e.g.:
 *
 * <pre class="code">
 * &lt;bean id="entityManagerFactory" class="org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean"&gt;
 *   ...
 *   &lt;property name="jpaPropertyMap"&gt;
 * 	   &lt;map>
 *       &lt;entry key="hibernate.resource.beans.container"&gt;
 * 	       &lt;bean class="org.springframework.orm.hibernate5.SpringBeanContainer"/&gt;
 * 	     &lt;/entry&gt;
 * 	   &lt;/map&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * Or in Java-based JPA configuration:
 *
 * <pre class="code">
 * LocalContainerEntityManagerFactoryBean emfb = ...
 * emfb.getJpaPropertyMap().put(AvailableSettings.BEAN_CONTAINER, new SpringBeanContainer(beanFactory));
 * </pre>
 *
 * Please note that Spring's {@link LocalSessionFactoryBean} is an immediate alternative
 * to {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean} for common
 * JPA purposes: In particular with Hibernate 5.3, the Hibernate {@code SessionFactory}
 * will natively expose the JPA {@code EntityManagerFactory} interface as well, and
 * Hibernate {@code BeanContainer} integration will be registered out of the box.
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @see LocalSessionFactoryBean#setBeanFactory
 * @see LocalSessionFactoryBuilder#setBeanContainer
 * @see org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean#setJpaPropertyMap
 * @see org.hibernate.cfg.AvailableSettings#BEAN_CONTAINER
 */
public final class SpringBeanContainer implements BeanContainer {

	private static final Log logger = LogFactory.getLog(SpringBeanContainer.class);

	private final ConfigurableListableBeanFactory beanFactory;

	private final Map<Object, SpringContainedBean<?>> beanCache = new ConcurrentReferenceHashMap<>();


	/**
	 * Instantiate a new SpringBeanContainer for the given bean factory.
	 * @param beanFactory the Spring bean factory to delegate to
	 */
	public SpringBeanContainer(ConfigurableListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "ConfigurableListableBeanFactory is required");
		this.beanFactory = beanFactory;
	}


	@Override
	@SuppressWarnings("unchecked")
	public <B> ContainedBean<B> getBean(
			Class<B> beanType, LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {

		SpringContainedBean<?> bean;
		if (lifecycleOptions.canUseCachedReferences()) {
			bean = this.beanCache.get(beanType);
			if (bean == null) {
				bean = createBean(beanType, lifecycleOptions, fallbackProducer);
				this.beanCache.put(beanType, bean);
			}
		}
		else {
			bean = createBean(beanType, lifecycleOptions, fallbackProducer);
		}
		return (SpringContainedBean<B>) bean;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <B> ContainedBean<B> getBean(
			String name, Class<B> beanType, LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {

		if (!this.beanFactory.containsBean(name)) {
			return getBean(beanType, lifecycleOptions, fallbackProducer);
		}

		SpringContainedBean<?> bean;
		if (lifecycleOptions.canUseCachedReferences()) {
			bean = this.beanCache.get(name);
			if (bean == null) {
				bean = createBean(name, beanType, lifecycleOptions, fallbackProducer);
				this.beanCache.put(name, bean);
			}
		}
		else {
			bean = createBean(name, beanType, lifecycleOptions, fallbackProducer);
		}
		return (SpringContainedBean<B>) bean;
	}

	@Override
	public void stop() {
		this.beanCache.values().forEach(SpringContainedBean::destroyIfNecessary);
		this.beanCache.clear();
	}


	private SpringContainedBean<?> createBean(
			Class<?> beanType, LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {

		try {
			if (lifecycleOptions.useJpaCompliantCreation()) {
				return new SpringContainedBean<>(
						this.beanFactory.createBean(beanType, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false),
						this.beanFactory::destroyBean);
			}
			else {
				return new SpringContainedBean<>(this.beanFactory.getBean(beanType));
			}
		}
		catch (BeansException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Falling back to Hibernate's default producer after bean creation failure for " +
						beanType + ": " + ex);
			}
			return new SpringContainedBean<>(fallbackProducer.produceBeanInstance(beanType));
		}
	}

	private SpringContainedBean<?> createBean(
			String name, Class<?> beanType, LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {

		try {
			if (lifecycleOptions.useJpaCompliantCreation()) {
				Object bean = this.beanFactory.autowire(beanType, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
				this.beanFactory.applyBeanPropertyValues(bean, name);
				bean = this.beanFactory.initializeBean(bean, name);
				return new SpringContainedBean<>(bean, beanInstance -> this.beanFactory.destroyBean(name, beanInstance));
			}
			else {
				return new SpringContainedBean<>(this.beanFactory.getBean(name, beanType));
			}
		}
		catch (BeansException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Falling back to Hibernate's default producer after bean creation failure for " +
						beanType + ": " + ex);
			}
			return new SpringContainedBean<>(fallbackProducer.produceBeanInstance(name, beanType));
		}
	}


	private static final class SpringContainedBean<B> implements ContainedBean<B> {

		private final B beanInstance;

		@Nullable
		private Consumer<B> destructionCallback;

		public SpringContainedBean(B beanInstance) {
			this.beanInstance = beanInstance;
		}

		public SpringContainedBean(B beanInstance, Consumer<B> destructionCallback) {
			this.beanInstance = beanInstance;
			this.destructionCallback = destructionCallback;
		}

		@Override
		public B getBeanInstance() {
			return this.beanInstance;
		}

		public void destroyIfNecessary() {
			if (this.destructionCallback != null) {
				this.destructionCallback.accept(this.beanInstance);
			}
		}
	}

}
