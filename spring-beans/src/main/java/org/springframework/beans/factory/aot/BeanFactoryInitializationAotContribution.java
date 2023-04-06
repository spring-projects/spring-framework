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

package org.springframework.beans.factory.aot;

import org.springframework.aot.generate.GenerationContext;

/**
 * AOT contribution from a {@link BeanFactoryInitializationAotProcessor} used to
 * initialize a bean factory.
 *
 * <p>Note: Beans implementing this interface will not have registration methods
 * generated during AOT processing unless they also implement
 * {@link org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter}.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see BeanFactoryInitializationAotProcessor
 */
@FunctionalInterface
public interface BeanFactoryInitializationAotContribution {

	/**
	 * Apply this contribution to the given {@link BeanFactoryInitializationCode}.
	 * @param generationContext the active generation context
	 * @param beanFactoryInitializationCode the bean factory initialization code
	 */
	void applyTo(GenerationContext generationContext,
			BeanFactoryInitializationCode beanFactoryInitializationCode);

}
