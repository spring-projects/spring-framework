/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.docs.integration.observability.httpserver.reactive;

import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;

@Controller
public class UserController {

	@ExceptionHandler(MissingUserException.class)
	ResponseEntity<Void> handleMissingUser(ServerWebExchange exchange, MissingUserException exception) {
		// We want to record this exception with the observation
		ServerRequestObservationContext.findCurrent(exchange.getAttributes())
				.ifPresent(context -> context.setError(exception));
		return ResponseEntity.notFound().build();
	}

	// @fold:on
	@SuppressWarnings("serial")
	static class MissingUserException extends RuntimeException {
	}
	// @fold:off

}
