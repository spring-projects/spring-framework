/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.expression;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.Environment;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.util.Assert;

/**
 * Read-only SpEL {@link PropertyAccessor} that knows how to retrieve properties
 * of a Spring {@link Environment} instance.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class EnvironmentAccessor implements PropertyAccessor {

	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return new Class<?>[] {Environment.class};
	}

	@Override
	public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		return (target instanceof Environment);
	}

	/**
	 * Access the given target object by resolving the given property name against
	 * the given target environment.
	 */
	@Override
	public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		Assert.state(target instanceof Environment, "Target must be of type Environment");
		return new TypedValue(((Environment) target).getProperty(name));
	}

	/**
	 * Read-only: returns {@code false}.
	 */
	@Override
	public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		return false;
	}

	@Override
	public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
			throws AccessException {

		throw new AccessException("The Environment is read-only");
	}

}
