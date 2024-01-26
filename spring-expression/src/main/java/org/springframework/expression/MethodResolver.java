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

import java.util.List;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

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
	@Nullable
	MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
			List<TypeDescriptor> argumentTypes) throws AccessException;

}
