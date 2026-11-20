/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.config;

/**
 * Callback for customizing a given bean definition.
 * Designed for use with a lambda expression or method reference.
 *
 * @author Juergen Hoeller
 * @since 5.0
 * @see org.springframework.beans.factory.support.BeanDefinitionBuilder#applyCustomizers
 */
@FunctionalInterface
public interface BeanDefinitionCustomizer {

	/**
	 * Customize the given bean definition.
	 */
	void customize(BeanDefinition bd);

}
