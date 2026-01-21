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

package org.springframework.test.context.aot;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;

/**
 * @author Sam Brannen
 * @since 6.2.4
 */
class AbstractAotContextLoader extends AbstractContextLoader implements AotContextLoader {

	@Override
	public final GenericApplicationContext loadContext(MergedContextConfiguration mergedConfig) {
		return new StaticApplicationContext();
	}

	@Override
	public final GenericApplicationContext loadContextForAotRuntime(MergedContextConfiguration mergedConfig,
			ApplicationContextInitializer<ConfigurableApplicationContext> initializer) {

		return loadContext(mergedConfig);
	}

	@Override
	protected final String getResourceSuffix() {
		throw new UnsupportedOperationException();
	}

}
