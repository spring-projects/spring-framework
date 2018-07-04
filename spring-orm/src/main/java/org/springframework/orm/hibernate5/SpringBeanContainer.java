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

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Spring's implementation of Hibernate 5.3's {@link BeanContainer} SPI,
 * delegating to a Spring {@link ConfigurableListableBeanFactory}.
 *
 * @author Juergen Hoeller
 * @since 5.1
 */
final class SpringBeanContainer implements BeanContainer {

	private final ConfigurableListableBeanFactory beanFactory;

	private final Map<Object, SpringContainedBean<?>> beanCache = new ConcurrentReferenceHashMap<>();


	public SpringBeanContainer(ConfigurableListableBeanFactory beanFactory) {
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
				bean = createBean(beanType, lifecycleOptions);
				this.beanCache.put(beanType, bean);
			}
		}
		else {
			bean = createBean(beanType, lifecycleOptions);
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
				bean = createBean(name, beanType, lifecycleOptions);
				this.beanCache.put(name, bean);
			}
		}
		else {
			bean = createBean(name, beanType, lifecycleOptions);
		}
		return (SpringContainedBean<B>) bean;
	}

	@Override
	public void stop() {
		this.beanCache.values().forEach(SpringContainedBean::destroyIfNecessary);
		this.beanCache.clear();
	}


	private SpringContainedBean<?> createBean(Class<?> beanType, LifecycleOptions lifecycleOptions) {
		if (lifecycleOptions.useJpaCompliantCreation()) {
			return new SpringContainedBean<>(
					this.beanFactory.createBean(beanType, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false),
					this.beanFactory::destroyBean);
		}
		else {
			return new SpringContainedBean<>(this.beanFactory.getBean(beanType));
		}
	}

	private SpringContainedBean<?> createBean(String name, Class<?> beanType, LifecycleOptions lifecycleOptions) {
		if (lifecycleOptions.useJpaCompliantCreation()) {
			Object bean = this.beanFactory.autowire(beanType, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
			this.beanFactory.applyBeanPropertyValues(bean, name);
			this.beanFactory.initializeBean(bean, name);
			return new SpringContainedBean<>(bean, beanInstance -> this.beanFactory.destroyBean(name, beanInstance));
		}
		else {
			return new SpringContainedBean<>(this.beanFactory.getBean(name, beanType));
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
