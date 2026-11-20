/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;

/**
 * A constructor resolver attempts to locate a constructor and returns a
 * {@link ConstructorExecutor} that can be used to invoke that constructor.
 *
 * <p>The {@code ConstructorExecutor} will be cached, but if it becomes stale the
 * resolvers will be called again.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @since 3.0
 * @see ConstructorExecutor
 * @see MethodResolver
 */
@FunctionalInterface
public interface ConstructorResolver {

	/**
	 * Within the supplied context, resolve a suitable constructor on the
	 * supplied type that can handle the specified arguments.
	 * <p>Returns a {@link ConstructorExecutor} that can be used to invoke that
	 * constructor (or {@code null} if no constructor could be found).
	 * @param context the current evaluation context
	 * @param typeName the fully-qualified name of the type upon which to look
	 * for the constructor
	 * @param argumentTypes the types of arguments that the constructor must be
	 * able to handle
	 * @return a {@code ConstructorExecutor} that can invoke the constructor,
	 * or {@code null} if the constructor cannot be found
	 */
	@Nullable ConstructorExecutor resolve(EvaluationContext context, String typeName, List<TypeDescriptor> argumentTypes)
			throws AccessException;

}
