/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.core.retry;

/**
 * Strategy interface to define a retry policy.
 *
 * @author Mahmoud Ben Hassine
 * @since 7.0
 */
public interface RetryPolicy {

	/**
	 * Start a new retry execution.
	 * @return a fresh {@link RetryExecution} ready to be used
	 */
	RetryExecution start();

}
