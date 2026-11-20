/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.config;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.AbstractComponentDefinition;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.parsing.ComponentDefinition}
 * implementation that holds a pointcut definition.
 *
 * @author Rob Harrop
 * @since 2.0
 */
public class PointcutComponentDefinition extends AbstractComponentDefinition {

	private final String pointcutBeanName;

	private final BeanDefinition pointcutDefinition;

	private final String description;


	public PointcutComponentDefinition(String pointcutBeanName, BeanDefinition pointcutDefinition, String expression) {
		Assert.notNull(pointcutBeanName, "Bean name must not be null");
		Assert.notNull(pointcutDefinition, "Pointcut definition must not be null");
		Assert.notNull(expression, "Expression must not be null");
		this.pointcutBeanName = pointcutBeanName;
		this.pointcutDefinition = pointcutDefinition;
		this.description = "Pointcut <name='" + pointcutBeanName + "', expression=[" + expression + "]>";
	}


	@Override
	public String getName() {
		return this.pointcutBeanName;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public BeanDefinition[] getBeanDefinitions() {
		return new BeanDefinition[] {this.pointcutDefinition};
	}

	@Override
	public @Nullable Object getSource() {
		return this.pointcutDefinition.getSource();
	}

}
