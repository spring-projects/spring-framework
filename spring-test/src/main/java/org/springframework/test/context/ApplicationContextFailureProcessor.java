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

package org.springframework.test.context;

import org.springframework.context.ApplicationContext;

/**
 * Strategy for components that process failures related to application contexts
 * within the <em>Spring TestContext Framework</em>.
 *
 * <p>Implementations must be registered via the
 * {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
 * mechanism.
 *
 * @author Sam Brannen
 * @since 6.0
 * @see ContextLoadException
 */
public interface ApplicationContextFailureProcessor {

	/**
	 * Invoked when a failure was encountered while attempting to load an
	 * {@link ApplicationContext}.
	 * <p>Implementations of this method must not throw any exceptions. Consequently,
	 * any exception thrown by an implementation of this method will be ignored, though
	 * potentially logged.
	 * @param context the application context that did not load successfully
	 * @param exception the exception thrown while loading the application context
	 */
	void processLoadFailure(ApplicationContext context, Throwable exception);

}
