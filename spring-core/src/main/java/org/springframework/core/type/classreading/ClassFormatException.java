/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.type.classreading;

import java.io.IOException;

import org.springframework.core.io.Resource;

/**
 * Exception that indicates an incompatible class format encountered
 * in a class file during metadata reading.
 *
 * @author Juergen Hoeller
 * @since 6.1.2
 * @see MetadataReaderFactory#getMetadataReader(Resource)
 * @see ClassFormatError
 */
@SuppressWarnings("serial")
public class ClassFormatException extends IOException {

	/**
	 * Construct a new {@code ClassFormatException} with the
	 * supplied message.
	 * @param message the detail message
	 */
	public ClassFormatException(String message) {
		super(message);
	}

	/**
	 * Construct a new {@code ClassFormatException} with the
	 * supplied message and cause.
	 * @param message the detail message
	 * @param cause the root cause
	 */
	public ClassFormatException(String message, Throwable cause) {
		super(message, cause);
	}

}
