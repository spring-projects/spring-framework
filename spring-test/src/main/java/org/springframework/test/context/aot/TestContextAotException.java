/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.aot;

/**
 * Thrown if an error occurs during AOT build-time processing or AOT run-time
 * execution in the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @since 6.0
 */
@SuppressWarnings("serial")
public class TestContextAotException extends RuntimeException {

	/**
	 * Create a new {@code TestContextAotException}.
	 * @param message the detail message
	 */
	public TestContextAotException(String message) {
		super(message);
	}

	/**
	 * Create a new {@code TestContextAotException}.
	 * @param message the detail message
	 * @param cause the root cause
	 */
	public TestContextAotException(String message, Throwable cause) {
		super(message, cause);
	}

}
