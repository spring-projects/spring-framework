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

package org.springframework.orm.jpa.hibernate;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.spi.TypeBootstrapContext;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Spring's implementation of Hibernate's {@link BeanContainer} SPI,
 * delegating to a Spring {@link ConfigurableListableBeanFactory}.
 *
 * <p>Auto-configured by {@link LocalSessionFactoryBean#setBeanFactory},
 * programmatically supported via {@link LocalSessionFactoryBuilder#setBeanContainer},
 * and manually configurable through a "hibernate.resource.beans.container" entry
 * in JPA properties, for example:
 *
 * <pre class="code">
 * &lt;bean id="entityManagerFactory" class="org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean"&gt;
 *   ...
 *   &lt;property name="jpaPropertyMap"&gt;
 * 	   &lt;map&gt;
 *       &lt;entry key="hibernate.resource.beans.container"&gt;
 * 	       &lt;bean class="org.springframework.orm.jpa.hibernate.SpringBeanContainer"/&gt;
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
 * to {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean} for
 * common JPA purposes: The Hibernate {@code SessionFactory} will natively expose the JPA
 * {@code EntityManagerFactory} interface as well, and Hibernate {@code BeanContainer}
 * integration will be registered out of the box.
 *
 * @author Juergen Hoeller
 * @author Yanming Zhou
 * @since 7.0
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


	@SuppressWarnings("unchecked")
	@Override
	public <B> ContainedBean<B> getBean(
			Class<B> beanType, LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {

		return (SpringContainedBean<B>) (lifecycleOptions.canUseCachedReferences() ?
				this.beanCache.computeIfAbsent(beanType, key -> createBean(beanType, false, lifecycleOptions, fallbackProducer)) :
				createBean(beanType, false, lifecycleOptions, fallbackProducer));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <B> ContainedBean<B> getBean(
			String name, Class<B> beanType, LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {

		return (SpringContainedBean<B>) (lifecycleOptions.canUseCachedReferences() ?
				this.beanCache.computeIfAbsent(name, key -> createBean(name, beanType, lifecycleOptions, fallbackProducer)) :
				createBean(name, beanType, lifecycleOptions, fallbackProducer));
	}

	@SuppressWarnings("unchecked")
	// @Override - on Hibernate 8.0
	public <B> ContainedBean<B> getBootstrapSafeBean(
			Class<B> beanType, LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {

		return (SpringContainedBean<B>) (lifecycleOptions.canUseCachedReferences() ?
				this.beanCache.computeIfAbsent(beanType, key -> createBean(beanType, false, lifecycleOptions, fallbackProducer)) :
				createBean(beanType, true, lifecycleOptions, fallbackProducer));
	}

	// @Override - on Hibernate 8.0
	public void releaseBean(ManagedBean<?> bean) {
		if (bean instanceof SpringContainedBean<?> contained) {
			this.beanCache.values().removeAll(Collections.singleton(contained));
			contained.destroyIfNecessary();
		}
	}

	// @Override - on Hibernate 8.0
	public <B> ContainedBean<B> getBootstrapSafeBean(
			Class<B> beanType, LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {

		// Fallback implementation for Hibernate 8.0 runtime compatibility
		return getBean(beanType, lifecycleOptions, fallbackProducer);
	}

	// @Override - on Hibernate 8.0
	public void releaseBean(ManagedBean<?> bean) {
		if (bean instanceof SpringContainedBean<?> contained) {
			this.beanCache.values().removeAll(Collections.singleton(contained));
			contained.destroyIfNecessary();
		}
	}

	@Override
	public void stop() {
		this.beanCache.values().forEach(SpringContainedBean::destroyIfNecessary);
		this.beanCache.clear();
	}


	private <B> SpringContainedBean<B> createBean(
			Class<B> beanType, boolean lazy, LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {

		try {
			if (lifecycleOptions.useJpaCompliantCreation() && !beanType.isInterface()) {
				B bean = null;
				if (fallbackProducer instanceof TypeBootstrapContext) {
					// Special Hibernate type construction rules, including TypeBootstrapContext resolution.
					bean = fallbackProducer.produceBeanInstance(beanType);
				}
				if (bean != null) {
					this.beanFactory.autowireBean(bean);
					new SpringContainedBean<>(beanType, bean, this.beanFactory::destroyBean);
				}
				return (lazy ?
						new SpringContainedBean<>(
								beanType, () -> this.beanFactory.createBean(beanType), this.beanFactory::destroyBean, fallbackProducer) :
						new SpringContainedBean<>(
								beanType, this.beanFactory.createBean(beanType), this.beanFactory::destroyBean));
			}
			else {
				return (lazy ?
						new SpringContainedBean<>(beanType, () -> this.beanFactory.getBean(beanType), fallbackProducer) :
						new SpringContainedBean<>(beanType, this.beanFactory.getBean(beanType)));
			}
		}
		catch (BeansException ex) {
			return new SpringContainedBean<>(beanType, createFallback(beanType, ex, fallbackProducer));
		}
	}

	@SuppressWarnings("unchecked")
	private <B> SpringContainedBean<B> createBean(
			String name, Class<B> beanType, LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {

		try {
			if (lifecycleOptions.useJpaCompliantCreation() && !beanType.isInterface()) {
				B bean = null;
				if (fallbackProducer instanceof TypeBootstrapContext) {
					// Special Hibernate type construction rules, including TypeBootstrapContext resolution.
					bean = fallbackProducer.produceBeanInstance(name, beanType);
				}
				if (this.beanFactory.containsBean(name)) {
					if (bean == null) {
						bean = (B) this.beanFactory.autowire(beanType, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
					}
					this.beanFactory.autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
					this.beanFactory.applyBeanPropertyValues(bean, name);
					bean = (B) this.beanFactory.initializeBean(bean, name);
					return new SpringContainedBean<>(beanType, bean, beanInstance -> this.beanFactory.destroyBean(name, beanInstance));
				}
				else if (bean != null) {
					// No bean found by name but constructed with TypeBootstrapContext rules
					this.beanFactory.autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
					bean = (B) this.beanFactory.initializeBean(bean, name);
					return new SpringContainedBean<>(beanType, bean, this.beanFactory::destroyBean);
				}
				else {
					// No bean found by name -> construct by type using createBean
					return new SpringContainedBean<>(
							beanType, this.beanFactory.createBean(beanType), this.beanFactory::destroyBean);
				}
			}
			else {
				return (this.beanFactory.containsBean(name) ?
						new SpringContainedBean<>(beanType, this.beanFactory.getBean(name, beanType)) :
						new SpringContainedBean<>(beanType, this.beanFactory.getBean(beanType)));
			}
		}
		catch (BeansException ex) {
			return new SpringContainedBean<>(beanType, createFallback(name, beanType, ex, fallbackProducer));
		}
	}

	private <B> B createFallback(Class<B> beanType, BeansException ex, BeanInstanceProducer fallbackProducer) {
		if (logger.isDebugEnabled()) {
			logger.debug("Falling back to Hibernate's default producer after bean creation failure for " +
					beanType + ": " + ex);
		}
		try {
			B bean = fallbackProducer.produceBeanInstance(beanType);
			if (bean != null) {
				this.beanFactory.autowireBean(bean);
			}
			return bean;
		}
		catch (RuntimeException ex2) {
			if (ex instanceof BeanCreationException) {
				if (logger.isDebugEnabled()) {
					logger.debug("Fallback producer failed for " + beanType + ": " + ex2);
				}
				// Rethrow original Spring exception from first attempt.
				throw ex;
			}
			else {
				// Throw fallback producer exception since original was probably NoSuchBeanDefinitionException.
				throw ex2;
			}
		}
	}

	private <B> B createFallback(String name, Class<B> beanType, BeansException ex, BeanInstanceProducer fallbackProducer) {
		if (logger.isDebugEnabled()) {
			logger.debug("Falling back to Hibernate's default producer after bean creation failure for " +
					beanType + " with name '" + name + "': " + ex);
		}
		try {
			B bean = fallbackProducer.produceBeanInstance(name, beanType);
			if (bean != null) {
				this.beanFactory.autowireBean(bean);
			}
			return bean;
		}
		catch (RuntimeException ex2) {
			if (ex instanceof BeanCreationException) {
				if (logger.isDebugEnabled()) {
					logger.debug("Fallback producer failed for " + beanType + " with name '" + name + "': " + ex2);
				}
				// Rethrow original Spring exception from first attempt.
				throw ex;
			}
			else {
				// Throw fallback producer exception since original was probably NoSuchBeanDefinitionException.
				throw ex2;
			}
		}
	}


	private final class SpringContainedBean<B> implements ContainedBean<B> {

		private final Class<B> beanClass;

		private volatile @Nullable B beanInstance;

		private @Nullable Supplier<B> beanSupplier;

		private @Nullable Consumer<B> destructionCallback;

		private @Nullable BeanInstanceProducer fallbackProducer;

		private volatile boolean fallback;

		private final Lock initializationLock = new ReentrantLock();

		public SpringContainedBean(Class<B> beanClass, B beanInstance) {
			this.beanClass = beanClass;
			this.beanInstance = beanInstance;
		}

		public SpringContainedBean(Class<B> beanClass, B beanInstance, Consumer<B> destructionCallback) {
			this.beanClass = beanClass;
			this.beanInstance = beanInstance;
			this.destructionCallback = destructionCallback;
		}

		public SpringContainedBean(Class<B> beanClass, Supplier<B> beanSupplier) {
			this.beanClass = beanClass;
			this.beanSupplier = beanSupplier;
		}

		public SpringContainedBean(Class<B> beanClass, Supplier<B> beanSupplier, BeanInstanceProducer fallbackProducer) {
			this.beanClass = beanClass;
			this.beanSupplier = beanSupplier;
			this.fallbackProducer = fallbackProducer;
		}

		public SpringContainedBean(Class<B> beanClass, Supplier<B> beanSupplier, Consumer<B> destructionCallback, BeanInstanceProducer fallbackProducer) {
			this.beanClass = beanClass;
			this.beanSupplier = beanSupplier;
			this.destructionCallback = destructionCallback;
			this.fallbackProducer = fallbackProducer;
		}

		@Override
		public @Nullable B getBeanInstance() {
			B instance = this.beanInstance;
			if (instance == null && !this.fallback) {
				this.initializationLock.lock();
				try {
					instance = this.beanInstance;
					if (this.beanSupplier != null) {
						try {
							instance = this.beanSupplier.get();
						}
						catch (BeansException ex) {
							if (this.fallbackProducer != null) {
								instance = createFallback(this.beanClass, ex, this.fallbackProducer);
								this.fallback = true;
							}
						}
					}
					this.beanInstance = instance;
				}
				finally {
					this.initializationLock.unlock();
				}
			}
			return instance;
		}

		@Override
		public Class<B> getBeanClass() {
			return this.beanClass;
		}

		// @Override - on Hibernate 8.0
		public void initialize() {
			getBeanInstance();
		}

		// @Override - on Hibernate 8.0
		public void release() {
			destroyIfNecessary();
		}

		public void destroyIfNecessary() {
			if (this.destructionCallback != null && !this.fallback) {
				B instance = this.beanInstance;
				if (instance != null) {
					this.destructionCallback.accept(instance);
				}
			}
		}
	}

}
