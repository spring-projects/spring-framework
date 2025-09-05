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

package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * {@link BeanRegistry} implementation that delegates to
 * {@link BeanDefinitionRegistry} and {@link ListableBeanFactory}.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
public class BeanRegistryAdapter implements BeanRegistry {

	private final BeanDefinitionRegistry beanRegistry;

	private final ListableBeanFactory beanFactory;

	private final Environment environment;

	private final Class<? extends BeanRegistrar> beanRegistrarClass;

	private final @Nullable MultiValueMap<String, BeanDefinitionCustomizer> customizers;


	public BeanRegistryAdapter(DefaultListableBeanFactory beanFactory, Environment environment,
			Class<? extends BeanRegistrar> beanRegistrarClass) {

		this(beanFactory, beanFactory, environment, beanRegistrarClass, null);
	}

	public BeanRegistryAdapter(BeanDefinitionRegistry beanRegistry, ListableBeanFactory beanFactory,
			Environment environment, Class<? extends BeanRegistrar> beanRegistrarClass) {

		this(beanRegistry, beanFactory, environment, beanRegistrarClass, null);
	}

	public BeanRegistryAdapter(BeanDefinitionRegistry beanRegistry, ListableBeanFactory beanFactory,
			Environment environment, Class<? extends BeanRegistrar> beanRegistrarClass,
			@Nullable MultiValueMap<String, BeanDefinitionCustomizer> customizers) {

		this.beanRegistry = beanRegistry;
		this.beanFactory = beanFactory;
		this.environment = environment;
		this.beanRegistrarClass = beanRegistrarClass;
		this.customizers = customizers;
	}


	@Override
	public void registerAlias(String name, String alias) {
		this.beanRegistry.registerAlias(name, alias);
	}

	@Override
	public <T> String registerBean(Class<T> beanClass) {
		String beanName = BeanDefinitionReaderUtils.uniqueBeanName(beanClass.getName(), this.beanRegistry);
		registerBean(beanName, beanClass);
		return beanName;
	}

	@Override
	public <T> String registerBean(Class<T> beanClass, Consumer<Spec<T>> customizer) {
		String beanName = BeanDefinitionReaderUtils.uniqueBeanName(beanClass.getName(), this.beanRegistry);
		registerBean(beanName, beanClass, customizer);
		return beanName;
	}

	@Override
	public <T> void registerBean(String name, Class<T> beanClass) {
		BeanRegistrarBeanDefinition beanDefinition = new BeanRegistrarBeanDefinition(beanClass, this.beanRegistrarClass);
		if (this.customizers != null && this.customizers.containsKey(name)) {
			for (BeanDefinitionCustomizer customizer : this.customizers.get(name)) {
				customizer.customize(beanDefinition);
			}
		}
		this.beanRegistry.registerBeanDefinition(name, beanDefinition);
	}

	@Override
	public <T> void registerBean(String name, Class<T> beanClass, Consumer<Spec<T>> spec) {
		BeanRegistrarBeanDefinition beanDefinition = new BeanRegistrarBeanDefinition(beanClass, this.beanRegistrarClass);
		spec.accept(new BeanSpecAdapter<>(beanDefinition, this.beanFactory));
		if (this.customizers != null && this.customizers.containsKey(name)) {
			for (BeanDefinitionCustomizer customizer : this.customizers.get(name)) {
				customizer.customize(beanDefinition);
			}
		}
		this.beanRegistry.registerBeanDefinition(name, beanDefinition);
	}

	@Override
	public void register(BeanRegistrar registrar) {
		Assert.notNull(registrar, "'registrar' must not be null");
		registrar.register(this, this.environment);
	}


	/**
	 * {@link RootBeanDefinition} subclass for {@code #registerBean} based
	 * registrations with constructors resolution match{@link BeanUtils#getResolvableConstructor}
	 * behavior. It also sets the bean registrar class as the source.
	 */
	@SuppressWarnings("serial")
	private static class BeanRegistrarBeanDefinition extends RootBeanDefinition {

		public BeanRegistrarBeanDefinition(Class<?> beanClass, Class<? extends BeanRegistrar> beanRegistrarClass) {
			super(beanClass);
			this.setSource(beanRegistrarClass);
			this.setAttribute("aotProcessingIgnoreRegistration", true);
		}

		public BeanRegistrarBeanDefinition(BeanRegistrarBeanDefinition original) {
			super(original);
		}

		@Override
		public Constructor<?> @Nullable [] getPreferredConstructors() {
			if (this.getInstanceSupplier() != null) {
				return null;
			}
			try {
				return new Constructor<?>[] { BeanUtils.getResolvableConstructor(getBeanClass()) };
			}
			catch (IllegalStateException ex) {
				return null;
			}
		}

		@Override
		public RootBeanDefinition cloneBeanDefinition() {
			return new BeanRegistrarBeanDefinition(this);
		}
	}


	private static class BeanSpecAdapter<T> implements Spec<T> {

		private final RootBeanDefinition beanDefinition;

		private final ListableBeanFactory beanFactory;

		public BeanSpecAdapter(RootBeanDefinition beanDefinition, ListableBeanFactory beanFactory) {
			this.beanDefinition = beanDefinition;
			this.beanFactory = beanFactory;
		}

		@Override
		public Spec<T> backgroundInit() {
			this.beanDefinition.setBackgroundInit(true);
			return this;
		}

		@Override
		public Spec<T> fallback() {
			this.beanDefinition.setFallback(true);
			return this;
		}

		@Override
		public Spec<T> primary() {
			this.beanDefinition.setPrimary(true);
			return this;
		}

		@Override
		public Spec<T> description(String description) {
			this.beanDefinition.setDescription(description);
			return this;
		}

		@Override
		public Spec<T> infrastructure() {
			this.beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			return this;
		}

		@Override
		public Spec<T> lazyInit() {
			this.beanDefinition.setLazyInit(true);
			return this;
		}

		@Override
		public Spec<T> notAutowirable() {
			this.beanDefinition.setAutowireCandidate(false);
			return this;
		}

		@Override
		public Spec<T> order(int order) {
			this.beanDefinition.setAttribute(AbstractBeanDefinition.ORDER_ATTRIBUTE, order);
			return this;
		}

		@Override
		public Spec<T> prototype() {
			this.beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
			return this;
		}

		@Override
		public Spec<T> supplier(Function<SupplierContext, T> supplier) {
			this.beanDefinition.setInstanceSupplier(() ->
					supplier.apply(new SupplierContextAdapter(this.beanFactory)));
			return this;
		}

		@Override
		public Spec<T> targetType(ParameterizedTypeReference<? extends T> targetType) {
			this.beanDefinition.setTargetType(ResolvableType.forType(targetType));
			return this;
		}

		@Override
		public Spec<T> targetType(ResolvableType targetType) {
			this.beanDefinition.setTargetType(targetType);
			return this;
		}
	}


	private static class SupplierContextAdapter implements SupplierContext {

		private final ListableBeanFactory beanFactory;

		public SupplierContextAdapter(ListableBeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public <T> T bean(Class<T> requiredType) throws BeansException {
			return this.beanFactory.getBean(requiredType);
		}

		@Override
		public <T> T bean(String name, Class<T> requiredType) throws BeansException {
			return this.beanFactory.getBean(name, requiredType);
		}

		@Override
		public <T> ObjectProvider<T> beanProvider(Class<T> requiredType) {
			return this.beanFactory.getBeanProvider(requiredType);
		}

		@Override
		public <T> ObjectProvider<T> beanProvider(ResolvableType requiredType) {
			return this.beanFactory.getBeanProvider(requiredType);
		}
	}

}
