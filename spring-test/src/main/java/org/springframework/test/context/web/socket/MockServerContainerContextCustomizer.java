/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.web.socket;

import jakarta.servlet.ServletContext;
import org.jspecify.annotations.Nullable;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link ContextCustomizer} that instantiates a new {@link MockServerContainer}
 * and stores it in the {@code ServletContext} under the attribute named
 * {@code "jakarta.websocket.server.ServerContainer"}.
 *
 * @author Sam Brannen
 * @since 4.3.1
 */
class MockServerContainerContextCustomizer implements ContextCustomizer {

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		if (context instanceof WebApplicationContext wac) {
			ServletContext sc = wac.getServletContext();
			if (sc != null) {
				sc.setAttribute("jakarta.websocket.server.ServerContainer", new MockServerContainer());
			}
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other != null && getClass() == other.getClass()));
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
