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

package org.springframework.beans.factory.support;

import java.lang.reflect.Executable;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.ResolvableType;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * A {@code RegisteredBean} represents a bean that has been registered with a
 * {@link BeanFactory}, but has not necessarily been instantiated. It provides
 * access to the bean factory that contains the bean as well as the bean name.
 * In the case of inner-beans, the bean name may have been generated.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public final class RegisteredBean {

	private final ConfigurableListableBeanFactory beanFactory;

	private final Supplier<String> beanName;

	private final boolean generatedBeanName;

	private final Supplier<RootBeanDefinition> mergedBeanDefinition;

	@Nullable
	private final RegisteredBean parent;


	private RegisteredBean(ConfigurableListableBeanFactory beanFactory, Supplier<String> beanName,
			boolean generatedBeanName, Supplier<RootBeanDefinition> mergedBeanDefinition,
			@Nullable RegisteredBean parent) {

		this.beanFactory = beanFactory;
		this.beanName = beanName;
		this.generatedBeanName = generatedBeanName;
		this.mergedBeanDefinition = mergedBeanDefinition;
		this.parent = parent;
	}


	/**
	 * Create a new {@link RegisteredBean} instance for a regular bean.
	 * @param beanFactory the source bean factory
	 * @param beanName the bean name
	 * @return a new {@link RegisteredBean} instance
	 */
	public static RegisteredBean of(ConfigurableListableBeanFactory beanFactory, String beanName) {
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		Assert.hasLength(beanName, "'beanName' must not be empty");
		return new RegisteredBean(beanFactory, () -> beanName, false,
				() -> (RootBeanDefinition) beanFactory.getMergedBeanDefinition(beanName),
				null);
	}

	/**
	 * Create a new {@link RegisteredBean} instance for a regular bean.
	 * @param beanFactory the source bean factory
	 * @param beanName the bean name
	 * @param mbd the pre-determined merged bean definition
	 * @return a new {@link RegisteredBean} instance
	 * @since 6.0.7
	 */
	static RegisteredBean of(ConfigurableListableBeanFactory beanFactory, String beanName, RootBeanDefinition mbd) {
		return new RegisteredBean(beanFactory, () -> beanName, false, () -> mbd, null);
	}

	/**
	 * Create a new {@link RegisteredBean} instance for an inner-bean.
	 * @param parent the parent of the inner-bean
	 * @param innerBean a {@link BeanDefinitionHolder} for the inner bean
	 * @return a new {@link RegisteredBean} instance
	 */
	public static RegisteredBean ofInnerBean(RegisteredBean parent, BeanDefinitionHolder innerBean) {
		Assert.notNull(innerBean, "'innerBean' must not be null");
		return ofInnerBean(parent, innerBean.getBeanName(), innerBean.getBeanDefinition());
	}

	/**
	 * Create a new {@link RegisteredBean} instance for an inner-bean.
	 * @param parent the parent of the inner-bean
	 * @param innerBeanDefinition the inner-bean definition
	 * @return a new {@link RegisteredBean} instance
	 */
	public static RegisteredBean ofInnerBean(RegisteredBean parent, BeanDefinition innerBeanDefinition) {
		return ofInnerBean(parent, null, innerBeanDefinition);
	}

	/**
	 * Create a new {@link RegisteredBean} instance for an inner-bean.
	 * @param parent the parent of the inner-bean
	 * @param innerBeanName the name of the inner bean or {@code null} to
	 * generate a name
	 * @param innerBeanDefinition the inner-bean definition
	 * @return a new {@link RegisteredBean} instance
	 */
	public static RegisteredBean ofInnerBean(RegisteredBean parent,
			@Nullable String innerBeanName, BeanDefinition innerBeanDefinition) {

		Assert.notNull(parent, "'parent' must not be null");
		Assert.notNull(innerBeanDefinition, "'innerBeanDefinition' must not be null");
		InnerBeanResolver resolver = new InnerBeanResolver(parent, innerBeanName, innerBeanDefinition);
		Supplier<String> beanName = (StringUtils.hasLength(innerBeanName) ?
				() -> innerBeanName : resolver::resolveBeanName);
		return new RegisteredBean(parent.getBeanFactory(), beanName,
				innerBeanName == null, resolver::resolveMergedBeanDefinition, parent);
	}


	/**
	 * Return the name of the bean.
	 * @return the beanName the bean name
	 */
	public String getBeanName() {
		return this.beanName.get();
	}

	/**
	 * Return if the bean name is generated.
	 * @return {@code true} if the name was generated
	 */
	public boolean isGeneratedBeanName() {
		return this.generatedBeanName;
	}

	/**
	 * Return the bean factory containing the bean.
	 * @return the bean factory
	 */
	public ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * Return the user-defined class of the bean.
	 * @return the bean class
	 */
	public Class<?> getBeanClass() {
		return ClassUtils.getUserClass(getBeanType().toClass());
	}

	/**
	 * Return the {@link ResolvableType} of the bean.
	 * @return the bean type
	 */
	public ResolvableType getBeanType() {
		return getMergedBeanDefinition().getResolvableType();
	}

	/**
	 * Return the merged bean definition of the bean.
	 * @return the merged bean definition
	 * @see ConfigurableListableBeanFactory#getMergedBeanDefinition(String)
	 */
	public RootBeanDefinition getMergedBeanDefinition() {
		return this.mergedBeanDefinition.get();
	}

	/**
	 * Return if this instance is for an inner-bean.
	 * @return if an inner-bean
	 */
	public boolean isInnerBean() {
		return this.parent != null;
	}

	/**
	 * Return the parent of this instance or {@code null} if not an inner-bean.
	 * @return the parent
	 */
	@Nullable
	public RegisteredBean getParent() {
		return this.parent;
	}

	/**
	 * Resolve the constructor or factory method to use for this bean.
	 * @return the {@link java.lang.reflect.Constructor} or {@link java.lang.reflect.Method}
	 */
	public Executable resolveConstructorOrFactoryMethod() {
		return new ConstructorResolver((AbstractAutowireCapableBeanFactory) getBeanFactory())
				.resolveConstructorOrFactoryMethod(getBeanName(), getMergedBeanDefinition());
	}

	/**
	 * Resolve an autowired argument.
	 * @param descriptor the descriptor for the dependency (field/method/constructor)
	 * @param typeConverter the TypeConverter to use for populating arrays and collections
	 * @param autowiredBeanNames a Set that all names of autowired beans (used for
	 * resolving the given dependency) are supposed to be added to
	 * @return the resolved object, or {@code null} if none found
	 * @since 6.0.9
	 */
	@Nullable
	public Object resolveAutowiredArgument(
			DependencyDescriptor descriptor, TypeConverter typeConverter, Set<String> autowiredBeanNames) {

		return new ConstructorResolver((AbstractAutowireCapableBeanFactory) getBeanFactory())
				.resolveAutowiredArgument(descriptor, descriptor.getDependencyType(),
						getBeanName(), autowiredBeanNames, typeConverter, true);
	}


	@Override
	public String toString() {
		return new ToStringCreator(this).append("beanName", getBeanName())
				.append("mergedBeanDefinition", getMergedBeanDefinition()).toString();
	}


	/**
	 * Resolver used to obtain inner-bean details.
	 */
	private static class InnerBeanResolver {

		private final RegisteredBean parent;

		@Nullable
		private final String innerBeanName;

		private final BeanDefinition innerBeanDefinition;

		@Nullable
		private volatile String resolvedBeanName;

		InnerBeanResolver(RegisteredBean parent, @Nullable String innerBeanName, BeanDefinition innerBeanDefinition) {
			Assert.isInstanceOf(AbstractAutowireCapableBeanFactory.class, parent.getBeanFactory());
			this.parent = parent;
			this.innerBeanName = innerBeanName;
			this.innerBeanDefinition = innerBeanDefinition;
		}

		String resolveBeanName() {
			String resolvedBeanName = this.resolvedBeanName;
			if (resolvedBeanName != null) {
				return resolvedBeanName;
			}
			resolvedBeanName = resolveInnerBean((beanName, mergedBeanDefinition) -> beanName);
			this.resolvedBeanName = resolvedBeanName;
			return resolvedBeanName;
		}

		RootBeanDefinition resolveMergedBeanDefinition() {
			return resolveInnerBean((beanName, mergedBeanDefinition) -> mergedBeanDefinition);
		}

		private <T> T resolveInnerBean(BiFunction<String, RootBeanDefinition, T> resolver) {
			// Always use a fresh BeanDefinitionValueResolver in case the parent merged bean definition has changed.
			BeanDefinitionValueResolver beanDefinitionValueResolver = new BeanDefinitionValueResolver(
					(AbstractAutowireCapableBeanFactory) this.parent.getBeanFactory(),
					this.parent.getBeanName(), this.parent.getMergedBeanDefinition());
			return beanDefinitionValueResolver.resolveInnerBean(this.innerBeanName, this.innerBeanDefinition, resolver);
		}
	}

}
