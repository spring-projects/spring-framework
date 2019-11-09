/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.util.Locale;

import reactor.core.publisher.Mono;

/**
 * Contract to resolve a view name to a {@link View} instance. The view name may
 * correspond to an HTML template or be generated dynamically.
 *
 * <p>The process of view resolution is driven through a ViewResolver-based
 * {@code HandlerResultHandler} implementation called
 * {@link ViewResolutionResultHandler
 * ViewResolutionResultHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see ViewResolutionResultHandler
 */
public interface ViewResolver {

	/**
	 * Resolve the view name to a View instance.
	 * @param viewName the name of the view to resolve
	 * @param locale the locale for the request
	 * @return the resolved view or an empty stream
	 */
	Mono<View> resolveViewName(String viewName, Locale locale);

}
