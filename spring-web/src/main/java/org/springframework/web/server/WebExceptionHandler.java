/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.server;

import reactor.core.publisher.Mono;

/**
 * Contract for handling exceptions during web server exchange processing.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebExceptionHandler {

	/**
	 * Handle the given exception. A completion signal through the return value
	 * indicates error handling is complete while an error signal indicates the
	 * exception is still not handled.
	 * @param exchange the current exchange
	 * @param ex the exception to handle
	 * @return {@code Mono<Void>} to indicate when exception handling is complete
	 */
	Mono<Void> handle(ServerWebExchange exchange, Throwable ex);

}
