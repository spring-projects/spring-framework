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

import org.springframework.lang.Nullable;

/**
 * Describes the type of method and constructor parameters, field declarations.
 *
 * @author Danny Thomas
 */
public interface TypeMetadata {

	/**
	 * Get the name of this type.
	 */
	String getTypeName();

	/**
	 * Determine if this type is primitive.
	 */
	boolean isPrimitive();

	/**
	 * Determine if this type is void.
	 */
	boolean isVoid();

	/**
	 * Retrieve the class metadata for this type.
	 * @return the {@link ClassMetadata} of the member clases of this class. Null if the type is not a class
	 * @throws ClassMetadataNotFoundException if class metadata for the interface classes
	 * could not be loaded.
	 * @since 6.x
	 */
	@Nullable
	ClassMetadata getClassMetadata();

}
