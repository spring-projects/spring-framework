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

package org.springframework.aot.generate;

import org.springframework.lang.Nullable;

/**
 * Thrown when value code generation fails.
 *
 * @author Stephane Nicoll
 * @since 6.1.2
 */
@SuppressWarnings("serial")
public class ValueCodeGenerationException extends RuntimeException {

	@Nullable
	private final Object value;

	protected ValueCodeGenerationException(String message, @Nullable Object value, @Nullable Throwable cause) {
		super(message, cause);
		this.value = value;
	}

	public ValueCodeGenerationException(@Nullable Object value, Throwable cause) {
		super(buildErrorMessage(value), cause);
		this.value = value;
	}

	private static String buildErrorMessage(@Nullable Object value) {
		StringBuilder message = new StringBuilder("Failed to generate code for '");
		message.append(value).append("'");
		if (value != null) {
			message.append(" with type ").append(value.getClass());
		}
		return message.toString();
	}

	/**
	 * Return the value that failed to be generated.
	 * @return the value
	 */
	@Nullable
	public Object getValue() {
		return this.value;
	}

}
