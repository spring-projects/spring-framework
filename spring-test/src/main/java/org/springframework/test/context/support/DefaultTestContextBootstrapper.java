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

package org.springframework.test.context.support;

import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * Default implementation of the {@link TestContextBootstrapper} SPI.
 *
 * <p>Uses {@link DelegatingSmartContextLoader} as the default {@link ContextLoader}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class DefaultTestContextBootstrapper extends AbstractTestContextBootstrapper {

	/**
	 * Returns {@link DelegatingSmartContextLoader}.
	 */
	@Override
	protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
		return DelegatingSmartContextLoader.class;
	}

}
