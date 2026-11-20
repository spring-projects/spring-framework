/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

import org.springframework.aot.generate.GenerationContext;

/**
 * AOT contribution from a {@link BeanFactoryInitializationAotProcessor} used to
 * initialize a bean factory.
 *
 * <p>Note: Beans implementing this interface will not have registration methods
 * generated during AOT processing unless they also implement
 * {@link org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter}.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see BeanFactoryInitializationAotProcessor
 */
@FunctionalInterface
public interface BeanFactoryInitializationAotContribution {

	/**
	 * Apply this contribution to the given {@link BeanFactoryInitializationCode}.
	 * @param generationContext the active generation context
	 * @param beanFactoryInitializationCode the bean factory initialization code
	 */
	void applyTo(GenerationContext generationContext,
			BeanFactoryInitializationCode beanFactoryInitializationCode);

}
