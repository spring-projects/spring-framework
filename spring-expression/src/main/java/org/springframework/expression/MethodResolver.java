/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;

/**
 * A method resolver attempts to locate a method and returns a
 * {@link MethodExecutor} that can be used to invoke that method.
 *
 * <p>The {@code MethodExecutor} will be cached, but if it becomes stale the
 * resolvers will be called again.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @since 3.0
 * @see MethodExecutor
 * @see ConstructorResolver
 */
@FunctionalInterface
public interface MethodResolver {

	/**
	 * Within the supplied context, resolve a suitable method on the supplied
	 * object that can handle the specified arguments.
	 * <p>Returns a {@link MethodExecutor} that can be used to invoke that method,
	 * or {@code null} if no method could be found.
	 * @param context the current evaluation context
	 * @param targetObject the object upon which the method is being called
	 * @param name the name of the method
	 * @param argumentTypes the types of arguments that the method must be able
	 * to handle
	 * @return a {@code MethodExecutor} that can invoke the method, or {@code null}
	 * if the method cannot be found
	 */
	@Nullable MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
			List<TypeDescriptor> argumentTypes) throws AccessException;

}
