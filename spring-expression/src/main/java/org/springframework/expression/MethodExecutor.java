/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression;

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
	TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException;

}
