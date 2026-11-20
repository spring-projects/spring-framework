/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.setup;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * A {@link ViewResolver} that always returns same View.
 *
 * @author Rob Winch
 * @since 6.2
 */
class StaticViewResolver implements ViewResolver {

	private final View view;

	public StaticViewResolver(View view) {
		this.view = view;
	}

	@Override
	public @Nullable View resolveViewName(String viewName, Locale locale) {
		return this.view;
	}
}
