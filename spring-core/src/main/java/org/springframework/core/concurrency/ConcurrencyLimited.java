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

/**
 * Functional callback representing a single unit of work to be executed under
 * the concurrency throttling of a {@link ConcurrencyLimitTemplate}.
 *
 * @author Geonhu Park
 * @since 7.1
 * @param <R> the result type (nullable)
 * @see ConcurrencyLimitTemplate
 */
@FunctionalInterface
public interface ConcurrencyLimited<R extends @Nullable Object> {

	/**
	 * Execute the concurrency-limited operation.
	 * @return the result (may be {@code null})
	 * @throws Throwable any error from the underlying operation
	 */
	R execute() throws Throwable;
}
