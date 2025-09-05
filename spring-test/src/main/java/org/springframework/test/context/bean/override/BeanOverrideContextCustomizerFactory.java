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

package org.springframework.test.context.bean.override;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.util.Assert;

/**
 * {@link ContextCustomizerFactory} implementation that provides support for
 * {@linkplain BeanOverride Bean Overrides}.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 * @see BeanOverride
 */
class BeanOverrideContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public @Nullable BeanOverrideContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {

		// Base the context name on the "closest" @ContextConfiguration declaration
		// within the type and enclosing class hierarchies of the test class.
		String contextName = configAttributes.get(0).getName();
		Set<BeanOverrideHandler> handlers = new LinkedHashSet<>();
		findBeanOverrideHandlers(testClass, contextName, handlers);
		if (handlers.isEmpty()) {
			return null;
		}
		return new BeanOverrideContextCustomizer(handlers);
	}

	private void findBeanOverrideHandlers(Class<?> testClass, @Nullable String contextName, Set<BeanOverrideHandler> handlers) {
		BeanOverrideHandler.findAllHandlers(testClass).stream()
				// If a handler does not specify a context name, it always gets applied.
				// Otherwise, the handler's context name must match the current context name.
				.filter(handler -> handler.getContextName().isEmpty() || handler.getContextName().equals(contextName))
				.forEach(handler -> Assert.state(handlers.add(handler),
						() -> "Duplicate BeanOverrideHandler discovered in test class %s: %s"
								.formatted(testClass.getName(), handler)));
	}

}
