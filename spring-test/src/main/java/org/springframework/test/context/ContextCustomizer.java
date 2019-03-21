/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.context.ConfigurableApplicationContext;

/**
 * Strategy interface for customizing {@link ConfigurableApplicationContext
 * application contexts} that are created and managed by the <em>Spring
 * TestContext Framework</em>.
 *
 * <p>Customizers are created by {@link ContextCustomizerFactory} implementations.
 *
 * <p>Implementations must implement correct {@code equals} and {@code hashCode}
 * methods since customizers form part of the {@link MergedContextConfiguration}
 * which is used as a cache key.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.3
 * @see ContextCustomizerFactory
 * @see org.springframework.test.context.support.AbstractContextLoader#customizeContext
 */
public interface ContextCustomizer {

	/**
	 * Customize the supplied {@code ConfigurableApplicationContext} <em>after</em>
	 * bean definitions have been loaded into the context but <em>before</em> the
	 * context has been refreshed.
	 * @param context the context to customize
	 * @param mergedConfig the merged context configuration
	 */
	void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig);

}
