/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

/**
 * A bean resolver can be registered with the evaluation context and will kick in
 * for bean references: {@code @myBeanName} and {@code &myBeanName} expressions.
 *
 * <p>The {@code &} variant syntax allows access to the factory bean where relevant.
 *
 * @author Andy Clement
 * @since 3.0.3
 */
@FunctionalInterface
public interface BeanResolver {

	/**
	 * Look up a bean by the given name and return a corresponding instance for it.
	 * <p>For attempting access to a factory bean, the name needs a {@code &} prefix.
	 * @param context the current evaluation context
	 * @param beanName the name of the bean to look up
	 * @return an object representing the bean
	 * @throws AccessException if there is an unexpected problem resolving the bean
	 */
	Object resolve(EvaluationContext context, String beanName) throws AccessException;

}
