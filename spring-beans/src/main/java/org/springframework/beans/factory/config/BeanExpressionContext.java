/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.config;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Context object for evaluating an expression within a bean definition.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class BeanExpressionContext {

	private final ConfigurableBeanFactory beanFactory;

	private final @Nullable Scope scope;


	public BeanExpressionContext(ConfigurableBeanFactory beanFactory, @Nullable Scope scope) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		this.scope = scope;
	}

	public final ConfigurableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	public final @Nullable Scope getScope() {
		return this.scope;
	}


	public boolean containsObject(String key) {
		return (this.beanFactory.containsBean(key) ||
				(this.scope != null && this.scope.resolveContextualObject(key) != null));
	}

	public @Nullable Object getObject(String key) {
		if (this.beanFactory.containsBean(key)) {
			return this.beanFactory.getBean(key);
		}
		else if (this.scope != null) {
			return this.scope.resolveContextualObject(key);
		}
		else {
			return null;
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof BeanExpressionContext that &&
				this.beanFactory == that.beanFactory && this.scope == that.scope));
	}

	@Override
	public int hashCode() {
		return this.beanFactory.hashCode();
	}

}
