/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

/**
 * A contract for inspecting and potentially modifying request data values such
 * as URL query parameters or form field values before they are rendered by a
 * view or before a redirect.
 *
 * <p>Implementations may use this contract for example as part of a solution
 * to provide data integrity, confidentiality, protection against cross-site
 * request forgery (CSRF), and others or for other tasks such as automatically
 * adding a hidden field to all forms and URLs.
 *
 * <p>View technologies that support this contract can obtain an instance to
 * delegate to via {@link RequestContext#getRequestDataValueProcessor()}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface RequestDataValueProcessor {

	/**
	 * Invoked when a new form action is rendered.
	 * @param exchange the current exchange
	 * @param action the form action
	 * @param httpMethod the form HTTP method
	 * @return the action to use, possibly modified
	 */
	String processAction(ServerWebExchange exchange, String action, String httpMethod);

	/**
	 * Invoked when a form field value is rendered.
	 * @param exchange the current exchange
	 * @param name the form field name
	 * @param value the form field value
	 * @param type the form field type ("text", "hidden", etc.)
	 * @return the form field value to use, possibly modified
	 */
	String processFormFieldValue(ServerWebExchange exchange, String name, String value, String type);

	/**
	 * Invoked after all form fields have been rendered.
	 * @param exchange the current exchange
	 * @return additional hidden form fields to be added, or {@code null}
	 */
	@Nullable
	Map<String, String> getExtraHiddenFields(ServerWebExchange exchange);

	/**
	 * Invoked when a URL is about to be rendered or redirected to.
	 * @param exchange the current exchange
	 * @param url the URL value
	 * @return the URL to use, possibly modified
	 */
	String processUrl(ServerWebExchange exchange, String url);

}
