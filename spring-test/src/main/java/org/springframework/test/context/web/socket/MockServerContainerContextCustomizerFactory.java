/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.web.socket;

import java.util.List;

import org.jspecify.annotations.Nullable;

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

	private static final boolean WEB_SOCKET_PRESENT = ClassUtils.isPresent("jakarta.websocket.server.ServerContainer",
			MockServerContainerContextCustomizerFactory.class.getClassLoader());


	@Override
	public @Nullable ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {

		if (WEB_SOCKET_PRESENT && isAnnotatedWithWebAppConfiguration(testClass)) {
			return new MockServerContainerContextCustomizer();
		}
		// Else, nothing to customize
		return null;
	}

	private static boolean isAnnotatedWithWebAppConfiguration(Class<?> testClass) {
		return TestContextAnnotationUtils.hasAnnotation(testClass, WebAppConfiguration.class);
	}

}
