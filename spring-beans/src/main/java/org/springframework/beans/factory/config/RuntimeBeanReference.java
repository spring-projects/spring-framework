/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Immutable placeholder class used for a property value object when it's
 * a reference to another bean in the factory, to be resolved at runtime.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see BeanDefinition#getPropertyValues()
 * @see org.springframework.beans.factory.BeanFactory#getBean(String)
 * @see org.springframework.beans.factory.BeanFactory#getBean(Class)
 */
public class RuntimeBeanReference implements BeanReference {

	private final String beanName;

	@Nullable
	private final Class<?> beanType;

	private final boolean toParent;

	@Nullable
	private Object source;


	/**
	 * Create a new RuntimeBeanReference to the given bean name.
	 * @param beanName name of the target bean
	 */
	public RuntimeBeanReference(String beanName) {
		this(beanName, false);
	}

	/**
	 * Create a new RuntimeBeanReference to the given bean name,
	 * with the option to mark it as reference to a bean in the parent factory.
	 * @param beanName name of the target bean
	 * @param toParent whether this is an explicit reference to a bean in the
	 * parent factory
	 */
	public RuntimeBeanReference(String beanName, boolean toParent) {
		Assert.hasText(beanName, "'beanName' must not be empty");
		this.beanName = beanName;
		this.beanType = null;
		this.toParent = toParent;
	}

	/**
	 * Create a new RuntimeBeanReference to a bean of the given type.
	 * @param beanType type of the target bean
	 * @since 5.2
	 */
	public RuntimeBeanReference(Class<?> beanType) {
		this(beanType, false);
	}

	/**
	 * Create a new RuntimeBeanReference to a bean of the given type,
	 * with the option to mark it as reference to a bean in the parent factory.
	 * @param beanType type of the target bean
	 * @param toParent whether this is an explicit reference to a bean in the
	 * parent factory
	 * @since 5.2
	 */
	public RuntimeBeanReference(Class<?> beanType, boolean toParent) {
		Assert.notNull(beanType, "'beanType' must not be empty");
		this.beanName = beanType.getName();
		this.beanType = beanType;
		this.toParent = toParent;
	}


	/**
	 * Return the requested bean name, or the fully-qualified type name
	 * in case of by-type resolution.
	 * @see #getBeanType()
	 */
	@Override
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return the requested bean type if resolution by type is demanded.
	 * @since 5.2
	 */
	@Nullable
	public Class<?> getBeanType() {
		return this.beanType;
	}

	/**
	 * Return whether this is an explicit reference to a bean in the parent factory.
	 */
	public boolean isToParent() {
		return this.toParent;
	}

	/**
	 * Set the configuration source {@code Object} for this metadata element.
	 * <p>The exact type of the object will depend on the configuration mechanism used.
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RuntimeBeanReference that)) {
			return false;
		}
		return (this.beanName.equals(that.beanName) && this.beanType == that.beanType &&
				this.toParent == that.toParent);
	}

	@Override
	public int hashCode() {
		int result = this.beanName.hashCode();
		result = 29 * result + (this.toParent ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return '<' + getBeanName() + '>';
	}

}
