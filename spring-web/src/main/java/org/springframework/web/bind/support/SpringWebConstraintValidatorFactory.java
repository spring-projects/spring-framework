/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind.support;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;

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
 * for example, in combination with JAX-RS or JAX-WS.
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
