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

import java.util.List;

/**
 * Interface that defines abstract access to the annotations of a constructor or method.
 *
 * @author Danny Thomas
 */
public interface ExecutableTypeMetadata extends AnnotatedTypeMetadata {

	/**
	 * Determine whether the underlying constructor is marked as 'final'.
	 */
	boolean isFinal();

	/**
	 * Determine whether the underlying constructor is marked as 'private'.
	 */
	boolean isPrivate();

	/**
	 * Determine whether the underlying method is overridable, i.e. not marked as static,
	 * final, or private.
	 */
	boolean isOverridable();

	/**
	 * Get all annotated constructor parameters.
	 */
	List<ParameterMetadata> getParameters();

	/**
	 * Get the constructor parameters annotated with the given name.
	 */
	List<ParameterMetadata> getAnnotatedParameters(String annotationName);

}
