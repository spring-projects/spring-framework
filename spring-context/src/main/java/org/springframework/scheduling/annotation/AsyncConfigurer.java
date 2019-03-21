/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

/**
 * Interface to be implemented by @{@link org.springframework.context.annotation.Configuration
 * Configuration} classes annotated with @{@link EnableAsync} that wish to customize the
 * {@link Executor} instance used when processing async method invocations or the
 * {@link AsyncUncaughtExceptionHandler} instance used to process exception thrown from
 * async method with {@code void} return type.
 *
 * <p>Consider using {@link AsyncConfigurerSupport} providing default implementations for
 * both methods if only one element needs to be customized. Furthermore, backward compatibility
 * of this interface will be insured in case new customization options are introduced
 * in the future.
 *
 * <p>See @{@link EnableAsync} for usage examples.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1
 * @see AbstractAsyncConfiguration
 * @see EnableAsync
 * @see AsyncConfigurerSupport
 */
public interface AsyncConfigurer {

	/**
	 * The {@link Executor} instance to be used when processing async
	 * method invocations.
	 */
	Executor getAsyncExecutor();

	/**
	 * The {@link AsyncUncaughtExceptionHandler} instance to be used
	 * when an exception is thrown during an asynchronous method execution
	 * with {@code void} return type.
	 */
	AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler();

}
