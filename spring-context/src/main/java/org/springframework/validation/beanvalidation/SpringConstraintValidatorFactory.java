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

package org.springframework.validation.beanvalidation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.Assert;

/**
 * JSR-303 {@link ConstraintValidatorFactory} implementation that delegates to a
 * Spring BeanFactory for creating autowired {@link ConstraintValidator} instances.
 *
 * <p>Note that this class is meant for programmatic use, not for declarative use
 * in a standard {@code validation.xml} file. Consider
 * {@link org.springframework.web.bind.support.SpringWebConstraintValidatorFactory}
 * for declarative use in a web application, for example, with JAX-RS or JAX-WS.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean(Class)
 * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
 */
public class SpringConstraintValidatorFactory implements ConstraintValidatorFactory {

	private final AutowireCapableBeanFactory beanFactory;

	private final @Nullable ConstraintValidatorFactory defaultConstraintValidatorFactory;


	/**
	 * Create a new SpringConstraintValidatorFactory for the given BeanFactory.
	 * @param beanFactory the target BeanFactory
	 */
	public SpringConstraintValidatorFactory(AutowireCapableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		this.defaultConstraintValidatorFactory = null;
	}

	/**
	 * Create a new SpringConstraintValidatorFactory for the given BeanFactory.
	 * @param beanFactory the target BeanFactory
	 * @param defaultConstraintValidatorFactory the default ConstraintValidatorFactory
	 * as exposed by the validation provider (for creating provider-internal validator
	 * implementations which might not be publicly accessible in a module path setup)
	 * @since 7.0.3
	 */
	public SpringConstraintValidatorFactory(
			AutowireCapableBeanFactory beanFactory, ConstraintValidatorFactory defaultConstraintValidatorFactory) {

		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		this.defaultConstraintValidatorFactory = defaultConstraintValidatorFactory;
	}


	@Override
	public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
		if (this.defaultConstraintValidatorFactory != null) {
			// Create provider-internal validator implementations through default ConstraintValidatorFactory.
			String providerModuleName = this.defaultConstraintValidatorFactory.getClass().getModule().getName();
			if (providerModuleName != null && providerModuleName.equals(key.getModule().getName())) {
				return this.defaultConstraintValidatorFactory.getInstance(key);
			}
		}
		return this.beanFactory.createBean(key);
	}

	@Override
	public void releaseInstance(ConstraintValidator<?, ?> instance) {
		this.beanFactory.destroyBean(instance);
	}

}
