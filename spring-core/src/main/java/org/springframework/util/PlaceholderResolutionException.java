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

package org.springframework.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;

/**
 * Thrown when the resolution of placeholder failed. This exception provides
 * the placeholder as well as the hierarchy of values that led to the issue.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
@SuppressWarnings("serial")
public class PlaceholderResolutionException extends IllegalArgumentException {

	private final String reason;

	private final String placeholder;

	private final List<String> values;

	/**
	 * Create an exception using the specified reason for its message.
	 * @param reason the reason for the exception, should contain the placeholder
	 * @param placeholder the placeholder
	 * @param value the original expression that led to the issue if available
	 */
	PlaceholderResolutionException(String reason, String placeholder, @Nullable String value) {
		this(reason, placeholder, (value != null ? List.of(value) : Collections.emptyList()));
	}

	private PlaceholderResolutionException(String reason, String placeholder, List<String> values) {
		super(buildMessage(reason, values));
		this.reason = reason;
		this.placeholder = placeholder;
		this.values = values;
	}

	private static String buildMessage(String reason, List<String> values) {
		StringBuilder sb = new StringBuilder();
		sb.append(reason);
		if (!CollectionUtils.isEmpty(values)) {
			String valuesChain = values.stream().map(value -> "\"" + value + "\"")
					.collect(Collectors.joining(" <-- "));
			sb.append(" in value %s".formatted(valuesChain));
		}
		return sb.toString();
	}

	/**
	 * Return a {@link PlaceholderResolutionException} that provides
	 * an additional parent value.
	 * @param value the parent value to add
	 * @return a new exception with the parent value added
	 */
	PlaceholderResolutionException withValue(String value) {
		List<String> allValues = new ArrayList<>(this.values);
		allValues.add(value);
		return new PlaceholderResolutionException(this.reason, this.placeholder, allValues);
	}

	/**
	 * Return the placeholder that could not be resolved.
	 * @return the unresolvable placeholder
	 */
	public String getPlaceholder() {
		return this.placeholder;
	}

	/**
	 * Return a contextualized list of the resolution attempts that led to this
	 * exception, where the first element is the value that generated this
	 * exception.
	 * @return the stack of values that led to this exception
	 */
	public List<String> getValues() {
		return this.values;
	}

}
