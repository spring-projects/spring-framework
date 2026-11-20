/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.support;

import java.lang.reflect.Constructor;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.AccessException;
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.util.ReflectionUtils;

/**
 * A simple ConstructorExecutor implementation that runs a constructor using reflective
 * invocation.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ReflectiveConstructorExecutor implements ConstructorExecutor {

	private final Constructor<?> ctor;

	private final @Nullable Integer varargsPosition;


	public ReflectiveConstructorExecutor(Constructor<?> ctor) {
		this.ctor = ctor;
		if (ctor.isVarArgs()) {
			this.varargsPosition = ctor.getParameterCount() - 1;
		}
		else {
			this.varargsPosition = null;
		}
	}

	@Override
	public TypedValue execute(EvaluationContext context, @Nullable Object... arguments) throws AccessException {
		try {
			ReflectionHelper.convertArguments(
					context.getTypeConverter(), arguments, this.ctor, this.varargsPosition);
			if (this.ctor.isVarArgs()) {
				arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(
						this.ctor.getParameterTypes(), arguments);
			}
			ReflectionUtils.makeAccessible(this.ctor);
			return new TypedValue(this.ctor.newInstance(arguments));
		}
		catch (Exception ex) {
			throw new AccessException("Problem invoking constructor: " + this.ctor, ex);
		}
	}

	public Constructor<?> getConstructor() {
		return this.ctor;
	}

}
