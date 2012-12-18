/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import org.springframework.util.Assert;

/**
 * Represents a problem with a bean definition configuration.
 * Mainly serves as common argument passed into a {@link ProblemReporter}.
 *
 * <p>May indicate a potentially fatal problem (an error) or just a warning.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see ProblemReporter
 */
public class Problem {

	private final String message;

	private final Location location;

	private final ParseState parseState;

	private final Throwable rootCause;


	/**
	 * Create a new instance of the {@link Problem} class.
	 * @param message	a message detailing the problem
	 * @param location the location within a bean configuration source that triggered the error
	 */
	public Problem(String message, Location location) {
		this(message, location, null, null);
	}

	/**
	 * Create a new instance of the {@link Problem} class.
	 * @param message	a message detailing the problem
	 * @param parseState the {@link ParseState} at the time of the error
	 * @param location the location within a bean configuration source that triggered the error
	 */
	public Problem(String message, Location location, ParseState parseState) {
		this(message, location, parseState, null);
	}

	/**
	 * Create a new instance of the {@link Problem} class.
	 * @param message    a message detailing the problem
	 * @param rootCause the underlying expection that caused the error (may be {@code null})
	 * @param parseState the {@link ParseState} at the time of the error
	 * @param location the location within a bean configuration source that triggered the error
	 */
	public Problem(String message, Location location, ParseState parseState, Throwable rootCause) {
		Assert.notNull(message, "Message must not be null");
		Assert.notNull(location, "Location must not be null");
		this.message = message;
		this.location = location;
		this.parseState = parseState;
		this.rootCause = rootCause;
	}


	/**
	 * Get the message detailing the problem.
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Get the location within a bean configuration source that triggered the error.
	 */
	public Location getLocation() {
		return this.location;
	}

	/**
	 * Get the description of the bean configuration source that triggered the error,
	 * as contained within this Problem's Location object.
	 * @see #getLocation()
	 */
	public String getResourceDescription() {
		return getLocation().getResource().getDescription();
	}

	/**
	 * Get the {@link ParseState} at the time of the error (may be {@code null}).
	 */
	public ParseState getParseState() {
		return this.parseState;
	}

	/**
	 * Get the underlying expection that caused the error (may be {@code null}).
	 */
	public Throwable getRootCause() {
		return this.rootCause;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Configuration problem: ");
		sb.append(getMessage());
		sb.append("\nOffending resource: ").append(getResourceDescription());
		if (getParseState() != null) {
			sb.append('\n').append(getParseState());
		}
		return sb.toString();
	}

}
