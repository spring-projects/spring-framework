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

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Strategy interface for customizing {@link ApplicationContext application contexts} that
 * are created and managed by the Spring TestContext Framework.
 *
 * <p>Customizers are loaded via {@link ContextCustomizerFactory} classes registered in
 * {@code spring.factories}.
 *
 * <p>Implementations should take care to implement correct {@code equals} and
 * {@code hashCode} methods since customizers form part of the
 * {@link MergedContextConfiguration} which is used as a cache key.
 *
 * @author Phillip Webb
 * @since 4.3
 * @see ContextCustomizerFactory
 * @see org.springframework.test.context.support.AbstractContextLoader
 */
public interface ContextCustomizer {

	/**
	 * Called <i>before</i> bean definitions are read to customize the
	 * {@link ConfigurableApplicationContext}.
	 * @param context the context that should be prepared
	 * @param mergedContextConfiguration the merged context configuration
	 */
	void prepareContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration);

}
