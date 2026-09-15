/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.beans;

/**
 * Exception thrown when a property path is not a well-formed property path
 * according to the grammar implemented by {@link PropertyPath}.
 *
 * <p>This signals a syntactically invalid path, as opposed to a
 * syntactically valid path that happens not to resolve against a particular
 * target object. The latter cases are reported as {@link NotReadablePropertyException}
 * or {@link NotWritablePropertyException} instead.
 *
 * <p>Extends {@link PropertyAccessException} so that a malformed path
 * encountered while binding a single property value can be collected into a
 * {@link PropertyBatchUpdateException} alongside other per-property failures,
 * rather than aborting the whole binding operation.
 *
 * @author Brian Clozel
 * @since 7.1
 * @see PropertyPath#parse(String)
 */
@SuppressWarnings("serial")
public class InvalidPropertyPathException extends PropertyAccessException {

	/**
	 * Error code that an {@code InvalidPropertyPathException} is registered with.
	 */
	public static final String ERROR_CODE = "invalidPropertyPath";


	private final String propertyPath;


	/**
	 * Create a new {@code InvalidPropertyPathException}.
	 * @param propertyPath the offending property path
	 * @param reason a description of the grammar rule that was violated
	 */
	public InvalidPropertyPathException(String propertyPath, String reason) {
		super("Invalid property path '" + propertyPath + "': " + reason, null);
		this.propertyPath = propertyPath;
	}


	/**
	 * Return the offending property path.
	 */
	public String getPropertyPath() {
		return this.propertyPath;
	}

	@Override
	public String getErrorCode() {
		return ERROR_CODE;
	}

}
