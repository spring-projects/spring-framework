/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.event;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * @author Sam Brannen
 * @since 5.3.3
 */
public class PublishedEventsExtension implements ParameterResolver {

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return PublishedEvents.class.isAssignableFrom(parameterContext.getParameter().getType());
	}

	@Override
	public PublishedEvents resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return new DefaultPublishedEvents();
	}

}
