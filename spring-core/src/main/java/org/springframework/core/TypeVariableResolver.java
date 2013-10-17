/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * Strategy interface that can be used to resolve {@link java.lang.reflect.TypeVariable}s.
 *
 * @author Phillip Webb
 * @since 4.0
 * @see ResolvableType
 * @see GenericTypeResolver
 */
interface TypeVariableResolver {

	/**
	 * Resolve the specified type variable.
	 * @param typeVariable the type variable to resolve (must not be {@code null})
	 * @return the resolved {@link java.lang.reflect.Type} for the variable or
	 * {@code null} if the variable cannot be resolved.
	 */
	Type resolveVariable(TypeVariable<?> typeVariable);

}
