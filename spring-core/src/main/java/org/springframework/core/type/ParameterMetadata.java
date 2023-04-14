/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.type;

/**
 * Method and constructor parameter {@link AnnotatedTypeMetadata}.
 *
 * @author Danny Thomas
 * @since 6.x
 */
public interface ParameterMetadata extends AnnotatedTypeMetadata {

	/**
	 * Get the name of the method that declares the parameter.
	 */
	String getDeclaringMethodName();

	/**
	 * Determine if this parameter is a constructor parameter.
	 * @return true if the parameter is for a constructor method
	 */
	default boolean isConstructorParameter() {
		return getDeclaringMethodName().equals("<init>");
	}

	/**
	 * Get the type of the method parameter.
	 */
	TypeMetadata getParameterType();

}
