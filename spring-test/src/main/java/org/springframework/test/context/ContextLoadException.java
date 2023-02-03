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
 * Exception thrown when an error occurs while a {@link SmartContextLoader}
 * attempts to load an {@link ApplicationContext}.
 *
 * <p>This exception provides access to the {@linkplain #getApplicationContext()
 * application context} that failed to load as well as the {@linkplain #getCause()
 * exception} caught while attempting to load that context.
 *
 * @author Sam Brannen
 * @since 6.0
 * @see SmartContextLoader#loadContext(MergedContextConfiguration)
 */
@SuppressWarnings("serial")
public class ContextLoadException extends Exception {

	private final ApplicationContext applicationContext;


	/**
	 * Create a new {@code ContextLoadException} for the supplied
	 * {@link ApplicationContext} and {@link Throwable}.
	 * @param applicationContext the application context that failed to load
	 * @param cause the exception caught while attempting to load that context
	 */
	public ContextLoadException(ApplicationContext applicationContext, Throwable cause) {
		super(cause);
		this.applicationContext = applicationContext;
	}


	/**
	 * Get the {@code ApplicationContext} that failed to load.
	 * <p>Clients must not retain a long-lived reference to the context returned
	 * from this method.
	 */
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

}
