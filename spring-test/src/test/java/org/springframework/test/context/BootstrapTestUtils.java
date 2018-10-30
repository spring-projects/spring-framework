/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context;

import org.springframework.test.context.support.DefaultBootstrapContext;

/**
 * Collection of test-related utility methods for working with {@link BootstrapContext
 * BootstrapContexts} and {@link TestContextBootstrapper TestContextBootstrappers}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public abstract class BootstrapTestUtils {

	private BootstrapTestUtils() {
		/* no-op */
	}

	public static BootstrapContext buildBootstrapContext(Class<?> testClass,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {
		return new DefaultBootstrapContext(testClass, cacheAwareContextLoaderDelegate);
	}

	public static TestContextBootstrapper resolveTestContextBootstrapper(BootstrapContext bootstrapContext) {
		return BootstrapUtils.resolveTestContextBootstrapper(bootstrapContext);
	}

}
