package org.springframework.expression.spel.reflection;

import java.lang.reflect.Array;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyReaderExecutor;

public class ReflectionPropertyReaderExecutorForArrayLength implements PropertyReaderExecutor {

	public ReflectionPropertyReaderExecutorForArrayLength() {
	}

	public Object execute(EvaluationContext context, Object target) throws AccessException {
		if (target.getClass().isArray()) {
			return Array.getLength(target);
		}
		throw new AccessException("Cannot determine length of a non-array type  '" + target.getClass() + "'");
	}

}
