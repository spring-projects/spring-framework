/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.aop.aspectj.AspectInstanceFactory} implementation
 * backed by a Spring {@link org.springframework.beans.factory.BeanFactory}.
 *
 * <p>Note that this may instantiate multiple times if using a prototype,
 * which probably won't give the semantics you expect.
 * Use a {@link LazySingletonAspectInstanceFactoryDecorator}
 * to wrap this to ensure only one new aspect comes back.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.factory.BeanFactory
 * @see LazySingletonAspectInstanceFactoryDecorator
 */
@SuppressWarnings("serial")
public class BeanFactoryAspectInstanceFactory implements MetadataAwareAspectInstanceFactory, Serializable {

	private final BeanFactory beanFactory;

	private final String name;

	private final AspectMetadata aspectMetadata;


	/**
	 * Create a BeanFactoryAspectInstanceFactory. AspectJ will be called to
	 * introspect to create AJType metadata using the type returned for the
	 * given bean name from the BeanFactory.
	 * @param beanFactory the BeanFactory to obtain instance(s) from
	 * @param name name of the bean
	 */
	public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name) {
		this(beanFactory, name, null);
	}

	/**
	 * Create a BeanFactoryAspectInstanceFactory, providing a type that AspectJ should
	 * introspect to create AJType metadata. Use if the BeanFactory may consider the type
	 * to be a subclass (as when using CGLIB), and the information should relate to a superclass.
	 * @param beanFactory the BeanFactory to obtain instance(s) from
	 * @param name the name of the bean
	 * @param type the type that should be introspected by AspectJ
	 * ({@code null} indicates resolution through {@link BeanFactory#getType} via the bean name)
	 */
	public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name, @Nullable Class<?> type) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.notNull(name, "Bean name must not be null");
		this.beanFactory = beanFactory;
		this.name = name;
		Class<?> resolvedType = type;
		if (type == null) {
			resolvedType = beanFactory.getType(name);
			Assert.notNull(resolvedType, "Unresolvable bean type - explicitly specify the aspect class");
		}
		this.aspectMetadata = new AspectMetadata(resolvedType, name);
	}


	@Override
	public Object getAspectInstance() {
		return this.beanFactory.getBean(this.name);
	}

	@Override
	@Nullable
	public ClassLoader getAspectClassLoader() {
		return (this.beanFactory instanceof ConfigurableBeanFactory ?
				((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader() :
				ClassUtils.getDefaultClassLoader());
	}

	@Override
	public AspectMetadata getAspectMetadata() {
		return this.aspectMetadata;
	}

	@Override
	@Nullable
	public Object getAspectCreationMutex() {
		if (this.beanFactory.isSingleton(this.name)) {
			// Rely on singleton semantics provided by the factory -> no local lock.
			return null;
		}
		else if (this.beanFactory instanceof ConfigurableBeanFactory) {
			// No singleton guarantees from the factory -> let's lock locally but
			// reuse the factory's singleton lock, just in case a lazy dependency
			// of our advice bean happens to trigger the singleton lock implicitly...
			return ((ConfigurableBeanFactory) this.beanFactory).getSingletonMutex();
		}
		else {
			return this;
		}
	}

	/**
	 * Determine the order for this factory's target aspect, either
	 * an instance-specific order expressed through implementing the
	 * {@link org.springframework.core.Ordered} interface (only
	 * checked for singleton beans), or an order expressed through the
	 * {@link org.springframework.core.annotation.Order} annotation
	 * at the class level.
	 * @see org.springframework.core.Ordered
	 * @see org.springframework.core.annotation.Order
	 */
	@Override
	public int getOrder() {
		Class<?> type = this.beanFactory.getType(this.name);
		if (type != null) {
			if (Ordered.class.isAssignableFrom(type) && this.beanFactory.isSingleton(this.name)) {
				return ((Ordered) this.beanFactory.getBean(this.name)).getOrder();
			}
			return OrderUtils.getOrder(type, Ordered.LOWEST_PRECEDENCE);
		}
		return Ordered.LOWEST_PRECEDENCE;
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + ": bean name '" + this.name + "'";
	}

}
