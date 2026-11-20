/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

import org.jspecify.annotations.Nullable;

/**
 * A {@code ConstructorExecutor} is built by a {@link ConstructorResolver} and
 * can be cached by the infrastructure to repeat an operation quickly without
 * going back to the resolvers.
 *
 * <p>For example, the particular constructor to execute on a class may be discovered
 * by a {@code ConstructorResolver} which then builds a {@code ConstructorExecutor}
 * that executes that constructor, and the resolved {@code ConstructorExecutor}
 * can be reused without needing to go back to the resolvers to discover the
 * constructor again.
 *
 * <p>If a {@code ConstructorExecutor} becomes stale, it should throw an
 * {@link AccessException} which signals to the infrastructure to go back to the
 * resolvers to ask for a new one.
 *
 * <p>A cached {@code ConstructorExecutor} is valid only for the
 * {@link EvaluationContext} configuration &mdash; in particular, the registered
 * {@link ConstructorResolver ConstructorResolvers} &mdash; under which it was resolved.
 * See {@link Expression} for the resulting contract on reusing a parsed expression
 * across different {@code EvaluationContext} instances.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @since 3.0
 * @see ConstructorResolver
 * @see MethodExecutor
 */
@FunctionalInterface
public interface ConstructorExecutor {

	/**
	 * Execute a constructor in the specified context using the specified arguments.
	 * @param context the evaluation context in which the constructor is being executed
	 * @param arguments the arguments to the constructor; should match (in terms
	 * of number and type) whatever the constructor will need to run
	 * @return the new object
	 * @throws AccessException if there is a problem executing the constructor or
	 * if this {@code ConstructorExecutor} has become stale
	 */
	TypedValue execute(EvaluationContext context, @Nullable Object... arguments) throws AccessException;

}
