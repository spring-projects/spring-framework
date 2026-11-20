/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.ast;

import org.springframework.expression.AccessException;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

/**
 * Represents a reference to a bean, for example {@code @orderService} or
 * {@code @'order.service'}.
 *
 * <p>For a {@link org.springframework.beans.factory.FactoryBean FactoryBean}, the
 * syntax {@code &orderServiceFactory} can be used to access the factory itself.
 *
 * @author Andy Clement
 * @author Sam Brannen
 */
public class BeanReference extends SpelNodeImpl {

	private static final String FACTORY_BEAN_PREFIX = "&";

	private final String beanName;


	public BeanReference(int startPos, int endPos, String beanName) {
		super(startPos, endPos);
		this.beanName = beanName;
	}


	/**
	 * Get the name of the referenced bean.
	 * @return the name of the referenced bean, potentially prefixed with
	 * {@code &} for a direct reference to a {@code FactoryBean}
	 * @since 6.2
	 */
	public final String getName() {
		return this.beanName;
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		BeanResolver beanResolver = state.getEvaluationContext().getBeanResolver();
		if (beanResolver == null) {
			throw new SpelEvaluationException(
					getStartPosition(), SpelMessage.NO_BEAN_RESOLVER_REGISTERED, this.beanName);
		}

		state.trackOperation();
		try {
			return new TypedValue(beanResolver.resolve(state.getEvaluationContext(), this.beanName));
		}
		catch (AccessException ex) {
			throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_BEAN_RESOLUTION,
				this.beanName, ex.getMessage());
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		if (!this.beanName.startsWith(FACTORY_BEAN_PREFIX)) {
			sb.append('@');
		}
		if (!this.beanName.contains(".")) {
			sb.append(this.beanName);
		}
		else {
			sb.append('\'').append(this.beanName).append('\'');
		}
		return sb.toString();
	}

}
