/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.setup;

import org.jspecify.annotations.Nullable;

import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;

/**
 * An empty method implementation of {@link MockMvcConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public abstract class MockMvcConfigurerAdapter implements MockMvcConfigurer {

	@Override
	public void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
	}

	@Override
	public @Nullable RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder, WebApplicationContext cxt) {
		return null;
	}

}
