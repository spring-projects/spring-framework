/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet;

import java.util.Locale;

/**
 * Provides additional information about a View such as whether it
 * performs redirects.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public interface SmartView extends View {

	/**
	 * Whether the view performs a redirect.
	 */
	boolean isRedirectView();

	/**
	 * In most cases, the {@link DispatcherServlet} uses {@link ViewResolver}s
	 * to resolve {@link View} instances. However, a special type of
	 * {@link View} may actually render a collection of fragments, each with its
	 * own model and view.
	 * <p>This callback provides such a view with the opportunity to resolve
	 * any nested views it contains prior to rendering.
	 * @param resolver to resolve views with
	 * @param locale the resolved locale for the request
	 * @throws Exception if any view cannot be resolved, or in case of problems
	 * creating an actual View instance
	 * @since 6.2
	 */
	default void resolveNestedViews(ViewResolver resolver, Locale locale) throws Exception {
		// no-op
	}

}
