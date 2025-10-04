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

package org.springframework.core.concurrency;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ConcurrencyThrottleSupport;

/**
 * Template-style API that throttles concurrent executions of user-provided callbacks
 * according to a configurable concurrency limit.
 *
 * <p>Blocking semantics are identical to {@link ConcurrencyThrottleSupport}:
 * when the configured limit is reached, additional callers will block until a
 * permit becomes available.
 *
 * <p>The default concurrency limit of this template is 1.
 *
 * @author Geonhu Park
 * @since 7.1
 * @see ConcurrencyThrottleSupport
 */
@SuppressWarnings("serial")
public class ConcurrencyLimitTemplate extends ConcurrencyThrottleSupport {

	/**
	 * Create a default {@code ConcurrencyLimitTemplate}
	 * with concurrency limit 1.
	 */
	public ConcurrencyLimitTemplate() {
		this(1);
	}

	/**
	 * Create a {@code ConcurrencyThrottleInterceptor}
	 * with the given concurrency limit.
	 */
	public ConcurrencyLimitTemplate(int concurrencyLimit) {
		setConcurrencyLimit(concurrencyLimit);
	}

	/**
	 * Execute the supplied callback under the configured concurrency limit.
	 * @param concurrencyLimited the unit of work to run
	 * @param <R> the result type (nullable)
	 * @return the callback's result (possibly {@code null})
	 * @throws Throwable any exception thrown by the callback
	 */
	public <R extends @Nullable Object> @Nullable R execute(ConcurrencyLimited<R> concurrencyLimited) throws Throwable {
		beforeAccess();
		try {
			return concurrencyLimited.execute();
		}
		finally {
			afterAccess();
		}
	}
}
