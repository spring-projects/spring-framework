/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Strategy interface for resolving the scope of bean definitions.
 *
 * @author Mark Fisher
 * @since 2.5
 * @see org.springframework.context.annotation.Scope
 */
@FunctionalInterface
public interface ScopeMetadataResolver {

	/**
	 * Resolve the {@link ScopeMetadata} appropriate to the supplied
	 * bean {@code definition}.
	 * <p>Implementations can of course use any strategy they like to
	 * determine the scope metadata, but some implementations that spring
	 * immediately to mind might be to use source level annotations
	 * present on {@link BeanDefinition#getBeanClassName() the class} of the
	 * supplied {@code definition}, or to use metadata present in the
	 * {@link BeanDefinition#attributeNames()} of the supplied {@code definition}.
	 * @param definition the target bean definition
	 * @return the relevant scope metadata; never {@code null}
	 */
	ScopeMetadata resolveScopeMetadata(BeanDefinition definition);

}
