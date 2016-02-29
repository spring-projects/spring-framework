/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.context.ConfigurableApplicationContext;

/**
 * Factory registered in {@code spring.factories} that is used to create
 * {@link ContextCustomizer ContextCustomizers}. Factories are called after
 * {@link ContextLoader ContextLoaders} have been triggered but before the
 * {@link MergedContextConfiguration} is created.
 *
 * @author Phillip Webb
 * @since 4.3
 */
public interface ContextCustomizerFactory {

	/**
	 * Get the {@link ContextCustomizer} (if any) that should be used to customize the
	 * {@link ConfigurableApplicationContext} when it is created.
	 * @param context the context customizer factory context
	 * @return a {@link ContextCustomizer} or {@code null}
	 */
	ContextCustomizer getContextCustomizer(ContextCustomizerFactoryContext context);

}
