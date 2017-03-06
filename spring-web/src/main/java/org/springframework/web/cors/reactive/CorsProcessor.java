/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.cors.reactive;

import reactor.core.publisher.Mono;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;

/**
 * A strategy that takes a reactive request and a {@link CorsConfiguration} and updates
 * the response.
 *
 * <p>This component is not concerned with how a {@code CorsConfiguration} is
 * selected but rather takes follow-up actions such as applying CORS validation
 * checks and either rejecting the response or adding CORS headers to the
 * response.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>
 */
public interface CorsProcessor {

	/**
	 * Process a request given a {@code CorsConfiguration}.
	 * @param configuration the applicable CORS configuration (possibly {@code null})
	 * @param exchange the current HTTP request / response
	 * @return a {@link Mono} emitting {@code false} if the request is rejected, {@code true} otherwise
	 */
	boolean processRequest(CorsConfiguration configuration, ServerWebExchange exchange);

}
