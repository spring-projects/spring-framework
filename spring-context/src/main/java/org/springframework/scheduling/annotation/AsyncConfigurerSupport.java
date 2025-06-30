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

package org.springframework.scheduling.annotation;

import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

/**
 * A convenience {@link AsyncConfigurer} that implements all methods
 * so that the defaults are used. Provides a backward compatible alternative
 * of implementing {@link AsyncConfigurer} directly.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @deprecated as of 6.0 in favor of implementing {@link AsyncConfigurer} directly
 */
@Deprecated(since = "6.0")
public class AsyncConfigurerSupport implements AsyncConfigurer {

	@Override
	public @Nullable Executor getAsyncExecutor() {
		return null;
	}

	@Override
	public @Nullable AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return null;
	}

}
