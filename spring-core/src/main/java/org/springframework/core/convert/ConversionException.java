/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.core.convert;

import org.springframework.core.NestedRuntimeException;

/**
 * Base class for exceptions thrown by the conversion system.
 *
 * @author Keith Donald
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class ConversionException extends NestedRuntimeException {

	/**
	 * Construct a new conversion exception.
	 * @param message the exception message
	 */
	public ConversionException(String message) {
		super(message);
	}

	/**
	 * Construct a new conversion exception.
	 * @param message the exception message
	 * @param cause the cause
	 */
	public ConversionException(String message, Throwable cause) {
		super(message, cause);
	}

}
