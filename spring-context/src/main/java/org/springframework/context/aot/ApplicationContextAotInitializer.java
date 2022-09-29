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

package org.springframework.context.aot;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Initializes a {@link ConfigurableApplicationContext} using AOT optimizations.
 *
 * @author Stephane Nicoll
 * @since 6.0
 * @deprecated in favor of {@link AotApplicationContextInitializer}
 */
@Deprecated(since = "6.0", forRemoval = true)
public class ApplicationContextAotInitializer {

	/**
	 * Initialize the specified application context using the specified
	 * {@link ApplicationContextInitializer} class names. Each class is
	 * expected to have a default constructor.
	 * @param context the context to initialize
	 * @param initializerClassNames the application context initializer class names
	 */
	public void initialize(ConfigurableApplicationContext context, String... initializerClassNames) {
		AotApplicationContextInitializer.forInitializerClasses(initializerClassNames).initialize(context);
	}

}
