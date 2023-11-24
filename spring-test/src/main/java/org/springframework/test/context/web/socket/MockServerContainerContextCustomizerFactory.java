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

package org.springframework.test.context.web.socket;

import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.ClassUtils;

/**
 * {@link ContextCustomizerFactory} which creates a {@link MockServerContainerContextCustomizer}
 * if WebSocket support is present in the classpath and the test class or one of
 * its enclosing classes is annotated or meta-annotated with
 * {@link WebAppConfiguration @WebAppConfiguration}.
 *
 * @author Sam Brannen
 * @since 4.3.1
 */
class MockServerContainerContextCustomizerFactory implements ContextCustomizerFactory {

	private static final boolean webSocketPresent = ClassUtils.isPresent("jakarta.websocket.server.ServerContainer",
			MockServerContainerContextCustomizerFactory.class.getClassLoader());


	@Override
	@Nullable
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {

		if (webSocketPresent && isAnnotatedWithWebAppConfiguration(testClass)) {
			return new MockServerContainerContextCustomizer();
		}
		// Else, nothing to customize
		return null;
	}

	private static boolean isAnnotatedWithWebAppConfiguration(Class<?> testClass) {
		return TestContextAnnotationUtils.hasAnnotation(testClass, WebAppConfiguration.class);
	}

}
