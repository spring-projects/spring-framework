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
	TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException;

}
