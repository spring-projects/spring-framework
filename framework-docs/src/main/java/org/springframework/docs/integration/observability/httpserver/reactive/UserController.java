/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		ServerRequestObservationContext.recordHandledException(exchange.getAttributes(), exception);
		return ResponseEntity.notFound().build();
	}

	// @fold:on
	@SuppressWarnings("serial")
	static class MissingUserException extends RuntimeException {
	}
	// @fold:off

}
