/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

import org.jspecify.annotations.Nullable;

/**
 * A {@code MethodExecutor} is built by a {@link MethodResolver} and can be cached
 * by the infrastructure to repeat an operation quickly without going back to the
 * resolvers.
 *
 * <p>For example, the particular method to execute on an object may be discovered
 * by a {@code MethodResolver} which then builds a {@code MethodExecutor} that
 * executes that method, and the resolved {@code MethodExecutor} can be reused
 * without needing to go back to the resolvers to discover the method again.
 *
 * <p>If a {@code MethodExecutor} becomes stale, it should throw an
 * {@link AccessException} which signals to the infrastructure to go back to the
 * resolvers to ask for a new one.
 *
 * <p>A cached {@code MethodExecutor} is valid only for the {@link EvaluationContext}
 * configuration &mdash; in particular, the registered {@link MethodResolver
 * MethodResolvers} &mdash; under which it was resolved. See {@link Expression} for the
 * resulting contract on reusing a parsed expression across different
 * {@code EvaluationContext} instances.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @since 3.0
 * @see MethodResolver
 * @see ConstructorExecutor
 */
@FunctionalInterface
public interface MethodExecutor {

	/**
	 * Execute a method in the specified context using the specified arguments.
	 * @param context the evaluation context in which the method is being executed
	 * @param target the target of the method invocation; may be {@code null} for
	 * {@code static} methods
	 * @param arguments the arguments to the method; should match (in terms of
	 * number and type) whatever the method will need to run
	 * @return the value returned from the method
	 * @throws AccessException if there is a problem executing the method or
	 * if this {@code MethodExecutor} has become stale
	 */
	TypedValue execute(EvaluationContext context, Object target, @Nullable Object... arguments) throws AccessException;

}
