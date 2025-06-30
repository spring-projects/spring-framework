/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.accept;

import org.springframework.web.server.ServerWebExchange;

/**
 * Contract to add handling of requests with a deprecated API version. Typically,
 * this involves use of response headers to send hints and information about
 * the deprecated version to clients.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface ApiVersionDeprecationHandler {

	/**
	 * Check if the requested API version is deprecated, and if so handle it
	 * accordingly, e.g. by setting response headers to signal the deprecation,
	 * to specify relevant dates and provide links to further details.
	 * @param version the resolved and parsed request version
	 * @param exchange the current exchange
	 */
	void handleVersion(Comparable<?> version, ServerWebExchange exchange);

}
