/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.expression;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.util.Assert;

/**
 * SpEL {@link PropertyAccessor} that knows how to access the beans and contextual
 * objects of a Spring {@link BeanExpressionContext}.
 *
 * @author Juergen Hoeller
 * @author Andy Clement
 * @since 3.0
 */
public class BeanExpressionContextAccessor implements PropertyAccessor {

	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return new Class<?>[] {BeanExpressionContext.class};
	}

	@Override
	public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		return (target instanceof BeanExpressionContext bec && bec.containsObject(name));
	}

	@Override
	public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		Assert.state(target instanceof BeanExpressionContext, "Target must be of type BeanExpressionContext");
		return new TypedValue(((BeanExpressionContext) target).getObject(name));
	}

	@Override
	public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		return false;
	}

	@Override
	public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
			throws AccessException {

		throw new AccessException("Beans in a BeanFactory are read-only");
	}

}
