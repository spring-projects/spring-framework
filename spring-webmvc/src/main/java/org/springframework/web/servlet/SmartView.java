/*
 * Copyright 2002-2024 the original author or authors.
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
