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

package org.springframework.web.reactive.config;

import java.util.function.Predicate;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;

/**
 * Helps to configure options related to blocking execution in WebFlux.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public class BlockingExecutionConfigurer {

	@Nullable
	private AsyncTaskExecutor executor;

	@Nullable
	private Predicate<HandlerMethod> blockingControllerMethodPredicate;


	/**
	 * Configure an executor to invoke blocking controller methods with.
	 * <p>By default, this is not set in which case controller methods are
	 * invoked without the use of an Executor.
	 * @param executor the task executor to use
	 */
	public BlockingExecutionConfigurer setExecutor(AsyncTaskExecutor executor) {
		this.executor = executor;
		return this;
	}

	/**
	 * Configure a predicate to decide if a controller method is blocking and
	 * should be called on a separate thread if an executor is
	 * {@linkplain #setExecutor configured}.
	 * <p>The default predicate matches controller methods whose return type is
	 * not recognized by the configured
	 * {@link org.springframework.core.ReactiveAdapterRegistry}.
	 * @param predicate the predicate to use
	 */
	public BlockingExecutionConfigurer setControllerMethodPredicate(Predicate<HandlerMethod> predicate) {
		this.blockingControllerMethodPredicate = predicate;
		return this;
	}


	@Nullable
	protected AsyncTaskExecutor getExecutor() {
		return this.executor;
	}

	@Nullable
	protected Predicate<HandlerMethod> getBlockingControllerMethodPredicate() {
		return this.blockingControllerMethodPredicate;
	}

}
