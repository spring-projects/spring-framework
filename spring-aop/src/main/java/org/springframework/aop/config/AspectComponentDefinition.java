/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.config;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;

/**
 * {@link org.springframework.beans.factory.parsing.ComponentDefinition}
 * that holds an aspect definition, including its nested pointcuts.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see #getNestedComponents()
 * @see PointcutComponentDefinition
 */
public class AspectComponentDefinition extends CompositeComponentDefinition {

	private final BeanDefinition[] beanDefinitions;

	private final BeanReference[] beanReferences;


	public AspectComponentDefinition(String aspectName, BeanDefinition @Nullable [] beanDefinitions,
			BeanReference @Nullable [] beanReferences, @Nullable Object source) {

		super(aspectName, source);
		this.beanDefinitions = (beanDefinitions != null ? beanDefinitions : new BeanDefinition[0]);
		this.beanReferences = (beanReferences != null ? beanReferences : new BeanReference[0]);
	}


	@Override
	public BeanDefinition[] getBeanDefinitions() {
		return this.beanDefinitions;
	}

	@Override
	public BeanReference[] getBeanReferences() {
		return this.beanReferences;
	}

}
