/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.aot;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;

/**
 * A public {@link FactoryBean} with a generic type.
 *
 * @author Stephane Nicoll
 */
public class GenericFactoryBean<T> implements FactoryBean<T> {

	private final Class<T> beanType;

	public GenericFactoryBean(Class<T> beanType) {
		this.beanType = beanType;
	}

	@Override
	public @Nullable T getObject() throws Exception {
		return BeanUtils.instantiateClass(this.beanType);
	}

	@Override
	public @Nullable Class<?> getObjectType() {
		return this.beanType;
	}
}
