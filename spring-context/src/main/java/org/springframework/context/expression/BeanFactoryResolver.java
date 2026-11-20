/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.expression;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.AccessException;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;

/**
 * SpEL {@link BeanResolver} that operates against a Spring {@link BeanFactory}.
 *
 * @author Juergen Hoeller
 * @since 3.0.4
 */
public class BeanFactoryResolver implements BeanResolver {

	private final BeanFactory beanFactory;


	/**
	 * Create a new {@code BeanFactoryResolver} for the given factory.
	 * @param beanFactory the {@code BeanFactory} to resolve bean names against
	 */
	public BeanFactoryResolver(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
	}


	@Override
	public Object resolve(EvaluationContext context, String beanName) throws AccessException {
		try {
			return this.beanFactory.getBean(beanName);
		}
		catch (BeansException ex) {
			throw new AccessException("Could not resolve bean reference against BeanFactory", ex);
		}
	}

}
