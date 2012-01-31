/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

/**
 * A method resolver attempts locate a method and returns a command executor that can be used to invoke that method.
 * The command executor will be cached but if it 'goes stale' the resolvers will be called again.
 *
 * @author Andy Clement
 * @since 3.0
 */
public interface MethodResolver {

	/**
	 * Within the supplied context determine a suitable method on the supplied object that can handle the
	 * specified arguments. Return a MethodExecutor that can be used to invoke that method
	 * (or <code>null</code> if no method could be found).
	 * @param context the current evaluation context
	 * @param targetObject the object upon which the method is being called
	 * @param argumentTypes the arguments that the constructor must be able to handle
	 * @return a MethodExecutor that can invoke the method, or null if the method cannot be found
	 */
	MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
			List<TypeDescriptor> argumentTypes) throws AccessException;

}
