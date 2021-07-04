/*
 * Copyright 2002-2013 the original author or authors.
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
 * MethodExecutors are built by the resolvers and can be cached by the infrastructure to
 * repeat an operation quickly without going back to the resolvers. For example, the
 * particular method to run on an object may be discovered by the reflection method
 * resolver - it will then build a MethodExecutor that executes that method and the
 * MethodExecutor can be reused without needing to go back to the resolver to discover
 * the method again.
 *
 * <p>They can become stale, and in that case should throw an AccessException:
 * This will cause the infrastructure to go back to the resolvers to ask for a new one.
 *
 * @author Andy Clement
 * @since 3.0
 */
public interface MethodExecutor {

	/**
	 * Execute a command using the specified arguments, and using the specified expression state.
	 * @param context the evaluation context in which the command is being executed
	 * @param target the target object of the call - null for static methods
	 * @param arguments the arguments to the executor, should match (in terms of number
	 * and type) whatever the command will need to run
	 * @return the value returned from execution
	 * @throws AccessException if there is a problem executing the command or the
	 * MethodExecutor is no longer valid
	 */
	TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException;

}
