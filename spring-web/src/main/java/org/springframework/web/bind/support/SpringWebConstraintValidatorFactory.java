/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.bind.support;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * JSR-303 {@link ConstraintValidatorFactory} implementation that delegates to
 * the current Spring {@link WebApplicationContext} for creating autowired
 * {@link ConstraintValidator} instances.
 *
 * <p>In contrast to
 * {@link org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory},
 * this variant is meant for declarative use in a standard {@code validation.xml} file,
 * e.g. in combination with JAX-RS or JAX-WS.
 *
 * @author Juergen Hoeller
 * @since 4.2.1
 * @see ContextLoader#getCurrentWebApplicationContext()
 * @see org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory
 */
public class SpringWebConstraintValidatorFactory implements ConstraintValidatorFactory {

	@Override
	public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
		return getWebApplicationContext().getAutowireCapableBeanFactory().createBean(key);
	}

	// Bean Validation 1.1 releaseInstance method
	@Override
	public void releaseInstance(ConstraintValidator<?, ?> instance) {
		getWebApplicationContext().getAutowireCapableBeanFactory().destroyBean(instance);
	}


	/**
	 * Retrieve the Spring {@link WebApplicationContext} to use.
	 * The default implementation returns the current {@link WebApplicationContext}
	 * as registered for the thread context class loader.
	 * @return the current WebApplicationContext (never {@code null})
	 * @see ContextLoader#getCurrentWebApplicationContext()
	 */
	protected WebApplicationContext getWebApplicationContext() {
		WebApplicationContext wac = ContextLoader.getCurrentWebApplicationContext();
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext registered for current thread - " +
					"consider overriding SpringWebConstraintValidatorFactory.getWebApplicationContext()");
		}
		return wac;
	}

}
